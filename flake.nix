{
  description = "MyBriefcase Bookmarks Android — Rust FFI + Android checks";

  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1.*.tar.gz";
    rust-overlay = {
      url = "github:oxalica/rust-overlay";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    crane.url = "github:ipetkov/crane";
    advisory-db = {
      url = "github:rustsec/advisory-db";
      flake = false;
    };
    nix-github-actions = {
      url = "github:nix-community/nix-github-actions";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, rust-overlay, crane, advisory-db, nix-github-actions }:
    let
      supportedSystems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forEachSupportedSystem = f: nixpkgs.lib.genAttrs supportedSystems (system: f {
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ rust-overlay.overlays.default self.overlays.default ];
        };
        inherit system;
      });
    in
    {
      overlays.default = final: prev: {
        rustToolchain = final.rust-bin.fromRustupToolchainFile ./rust-toolchain.toml;
      };

      checks = forEachSupportedSystem ({ pkgs, ... }:
        let
          craneLib = (crane.mkLib pkgs).overrideToolchain pkgs.rustToolchain;
          src = pkgs.lib.cleanSourceWith {
            src = ./rust;
            filter = path: type:
              (craneLib.filterCargoSources path type) ||
              (builtins.match ".*\\.udl$" path != null) ||
              (builtins.match ".*deny\\.toml$" path != null);
          };
          commonArgs = {
            inherit src;
            pname = "mybriefcase-bookmarks-ffi";
          };
          cargoArtifacts = craneLib.buildDepsOnly commonArgs;
        in
        {
          fmt = craneLib.cargoFmt { inherit src; };

          clippy = craneLib.cargoClippy (commonArgs // {
            inherit cargoArtifacts;
            cargoClippyExtraArgs = "--all-targets -- -D warnings";
          });

          test = craneLib.cargoTest (commonArgs // {
            inherit cargoArtifacts;
            cargoTestExtraArgs = "-- --test-threads=1";
          });

          deny = craneLib.cargoDeny { inherit src; };

          audit = craneLib.cargoAudit {
            inherit src advisory-db;
          };

          doc = craneLib.cargoDoc (commonArgs // {
            inherit cargoArtifacts;
            RUSTDOCFLAGS = "-D warnings";
            cargoDocExtraArgs = "--no-deps";
          });
        }
      );

      githubActions = nix-github-actions.lib.mkGithubMatrix {
        checks = nixpkgs.lib.getAttrs [ "x86_64-linux" ] self.checks;
      };

      apps = forEachSupportedSystem ({ pkgs, ... }:
        let
          nightlyToolchain = pkgs.rust-bin.selectLatestNightlyWith (toolchain:
            toolchain.default.override {
              extensions = [ "miri" "rust-src" ];
            }
          );
        in {
        miri = {
          type = "app";
          program = toString (pkgs.writeShellScript "miri" ''
            set -euo pipefail
            export PATH="${nightlyToolchain}/bin:$PATH"
            cargo miri test --manifest-path rust/Cargo.toml
          '');
        };

        gradle-lint = {
          type = "app";
          program = toString (pkgs.writeShellScript "gradle-lint" ''
            set -euo pipefail
            ./gradlew lint
          '');
        };

        gradle-test = {
          type = "app";
          program = toString (pkgs.writeShellScript "gradle-test" ''
            set -euo pipefail
            ./gradlew testDebugUnitTest
          '');
        };

        validate = {
          type = "app";
          program = toString (pkgs.writeShellScript "validate" ''
            exec nix develop --command validate
          '');
        };

        validate-all = {
          type = "app";
          program = toString (pkgs.writeShellScript "validate-all" ''
            exec nix develop --command validate-all
          '');
        };
      });

      devShells = forEachSupportedSystem ({ pkgs, ... }:
        let
          nightlyToolchain = pkgs.rust-bin.selectLatestNightlyWith (toolchain:
            toolchain.default.override {
              extensions = [ "miri" "rust-src" ];
            }
          );

          gradle-lint = pkgs.writeShellScriptBin "gradle-lint" ''
            set -euo pipefail
            ./gradlew lint
          '';

          gradle-test = pkgs.writeShellScriptBin "gradle-test" ''
            set -euo pipefail
            ./gradlew testDebugUnitTest
          '';

          miri = pkgs.writeShellScriptBin "miri" ''
            set -euo pipefail
            export PATH="${nightlyToolchain}/bin:$PATH"
            cargo miri test --manifest-path rust/Cargo.toml
          '';

          validate = pkgs.writeShellScriptBin "validate" ''
            set -euo pipefail
            echo "==> Nix flake checks (Rust fmt, clippy, test, deny, audit, doc)..."
            nix flake check --keep-going
            echo "==> Android lint..."
            gradle-lint
            echo "==> Android unit tests..."
            gradle-test
            echo "==> All validations passed!"
          '';

          validate-all = pkgs.writeShellScriptBin "validate-all" ''
            set -euo pipefail
            validate
            echo "==> Running Miri..."
            miri
            echo "==> All validations (including Miri) passed!"
          '';
        in {
        default = pkgs.mkShell {
          packages = with pkgs; [
            rustToolchain
            cargo-ndk
            rust-analyzer
            gradle-lint
            gradle-test
            miri
            validate
            validate-all
          ];

          shellHook = ''
            if [ -z "''${ANDROID_NDK_HOME:-}" ]; then
              NDK_DIR="$HOME/Library/Android/sdk/ndk"
              if [ -d "$NDK_DIR" ]; then
                export ANDROID_NDK_HOME="$NDK_DIR/$(ls "$NDK_DIR" | sort -V | tail -1)"
              fi
            fi
          '';
        };
      });
    };
}
