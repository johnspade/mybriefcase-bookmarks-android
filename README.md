# MyBriefcase Bookmarks Android

Android client for the MyBriefcase Bookmarks system — a local-first bookmark manager using Automerge CRDTs synced over Syncthing.

## Prerequisites

The project uses a **Nix flake** with [direnv](https://direnv.net/) to manage the Rust toolchain. Make sure you have Nix (with flakes enabled) and direnv installed, then:

```bash
direnv allow   # activates the dev shell automatically on cd
```

This provides the Rust toolchain, cargo-ndk, rust-analyzer, and validation scripts.

You also need:
- Android SDK with NDK installed (the dev shell auto-detects `~/Library/Android/sdk/ndk`)
- Java 17+ (for Gradle)

## Architecture: Native Rust Core

The app's core logic is a shared Rust crate (`mybriefcase-bookmarks-core`) that lives in the parent [mybriefcase-bookmarks](https://github.com/johnspade/mybriefcase-bookmarks) project and is used by both the web and Android clients. The `rust/ffi/` crate in this repo wraps it with a [UniFFI](https://mozilla.github.io/uniffi-rs/) layer, compiling to a native shared library (`libmybriefcase_bookmarks_ffi.so`). The Kotlin layer is a thin UI shell that calls into Rust through generated bindings.

This means **every build that produces a runnable APK must compile the Rust code** for the target Android ABIs (`arm64-v8a`, `x86_64`).

## Building

```bash
./gradlew assembleDebug
```

The Gradle `preBuild` task automatically invokes `cargo-ndk` to cross-compile the Rust FFI library. Without `cargo-ndk` on PATH (e.g. CI lint/test jobs), the task is skipped.

## Validation

Two validation tiers are available, matching CI:

| Command | What it runs |
|---------|-------------|
| `validate` | `nix flake check` + `gradle-lint` + `gradle-test` (fast, pre-commit) |
| `validate-all` | `validate` + `miri` (full CI equivalent) |

Both commands are available in the dev shell (via `direnv allow`) or without it:

```bash
nix run .#validate       # fast
nix run .#validate-all   # full
```

Individual steps:

| Command | What it runs |
|---------|-------------|
| `nix flake check` | Rust fmt, clippy, test, deny, audit, doc |
| `gradle-lint` | Android Lint |
| `gradle-test` | Android unit tests (`testDebugUnitTest`) |
| `miri` | Miri (nightly toolchain) |
