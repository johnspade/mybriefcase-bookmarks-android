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

      packages.x86_64-linux =
        let
          pkgs = import nixpkgs {
            system = "x86_64-linux";
            config.allowUnfree = true;
            config.android_sdk.accept_license = true;
            overlays = [ rust-overlay.overlays.default self.overlays.default ];
          };
        in {
          screenshot-image = import ./nix/screenshot-image.nix { inherit pkgs; };
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
            exec nix develop --command gradle-lint
          '');
        };

        gradle-test = {
          type = "app";
          program = toString (pkgs.writeShellScript "gradle-test" ''
            exec nix develop --command gradle-test
          '');
        };

        gradle-coverage = {
          type = "app";
          program = toString (pkgs.writeShellScript "gradle-coverage" ''
            exec nix develop --command gradle-coverage
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

        verify-screenshots = {
          type = "app";
          program = toString (pkgs.writeShellScript "verify-screenshots" ''
            exec nix develop --command verify-screenshots
          '');
        };

        record-screenshots = {
          type = "app";
          program = toString (pkgs.writeShellScript "record-screenshots" ''
            exec nix develop --command record-screenshots
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
            ./gradlew detekt spotlessCheck lint
          '';

          gradle-test = pkgs.writeShellScriptBin "gradle-test" ''
            set -euo pipefail
            cargo build --manifest-path rust/ffi/Cargo.toml --release
            ./gradlew testDebugUnitTest
          '';

          gradle-coverage = pkgs.writeShellScriptBin "gradle-coverage" ''
            set -euo pipefail
            ./gradlew koverVerify
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
            echo "==> Coverage verification..."
            gradle-coverage
            echo "==> All validations passed!"
          '';

          validate-all = pkgs.writeShellScriptBin "validate-all" ''
            set -euo pipefail
            validate
            echo "==> Screenshot verification..."
            verify-screenshots
            echo "==> Running Miri..."
            miri
            echo "==> All validations (including Miri) passed!"
          '';

          screenshot-docker = pkgs.writeShellScriptBin "screenshot-docker" ''
            set -euo pipefail
            if ! command -v docker &>/dev/null; then
              echo "Error: Docker is required for screenshot tests" >&2
              exit 1
            fi

            IMAGE="ghcr.io/johnspade/mybriefcase-bookmarks-android/roborazzi-screenshot-recorder:latest"
            echo "==> Pulling $IMAGE..."
            docker pull --platform linux/amd64 "$IMAGE"

            TASK="''${1:-verifyRoborazziDebug}"
            GIT_COMMON="$(git rev-parse --git-common-dir)"
            VOLUME_ARGS="-v $PWD:/project"
            if [ "$GIT_COMMON" != ".git" ]; then
              VOLUME_ARGS="$VOLUME_ARGS -v $GIT_COMMON:$GIT_COMMON"
            fi

            echo "==> Running $TASK via Docker (linux/amd64)..."
            docker run --platform linux/amd64 --rm \
              $VOLUME_ARGS \
              "$IMAGE" \
              bash -c "
                git config --global --add safe.directory /project
                cargo build --manifest-path rust/ffi/Cargo.toml --release
                ./gradlew $TASK -PskipRustBuild=true
              "
          '';

          verify-screenshots = pkgs.writeShellScriptBin "verify-screenshots" ''
            set -euo pipefail
            screenshot-docker verifyRoborazziDebug
            echo "==> Screenshots verified successfully!"
          '';

          record-screenshots = pkgs.writeShellScriptBin "record-screenshots" ''
            set -euo pipefail
            screenshot-docker recordRoborazziDebug
            echo "==> Screenshots recorded successfully!"
          '';
        in {
        default = pkgs.mkShell {
          packages = with pkgs; [
            rustToolchain
            cargo-ndk
            rust-analyzer
            gradle-lint
            gradle-test
            gradle-coverage
            miri
            validate
            validate-all
            screenshot-docker
            verify-screenshots
            record-screenshots
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
