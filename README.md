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

## Building

```bash
./gradlew assembleDebug
```

The Gradle build invokes `cargo-ndk` to cross-compile the Rust FFI library for Android targets.

## Validation

All CI checks can be run locally with a single command:

```bash
validate
```

Individual steps:

| Command | What it runs |
|---------|-------------|
| `nix flake check` | Rust fmt, clippy, test, deny, audit, doc |
| `gradle-lint` | Android Lint |
| `gradle-test` | Android unit tests (`testDebugUnitTest`) |
| `miri` | Miri (nightly toolchain) |
