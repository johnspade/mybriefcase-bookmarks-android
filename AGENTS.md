# Agent Guidelines

## Commit Messages

PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/) — CI enforces this. The PR title becomes the squash-merge commit message on main.

Format: `<type>: <description>` or `<type>(scope): <description>`

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`.

Use `!` after the type for breaking changes: `feat!: remove legacy endpoint`.

## Before Committing

Run `validate` before committing. All checks must pass — CI will reject the PR otherwise.

Two validation tiers:

| Command | What it runs |
|---------|-------------|
| `validate` | `nix flake check` + `gradle-lint` + `gradle-test` (fast, pre-commit) |
| `validate-all` | `validate` + `verify-screenshots` + `miri` (full CI equivalent) |

Individual steps:

| Command | What it runs |
|---------|-------------|
| `nix flake check` | Rust fmt, clippy, test, deny, audit, doc |
| `gradle-lint` | Android Lint |
| `gradle-test` | Android unit tests |
| `nix run .#verify-screenshots` | Verify golden PNGs via Docker (linux/amd64) |
| `nix run .#record-screenshots` | Re-record golden PNGs via Docker (linux/amd64) |
| `miri` | Miri (nightly toolchain) |

Never force-push to `main`. If a commit needs fixing, create a new commit instead of amending.

## Warnings

Fix compiler and clippy warnings properly instead of suppressing them with `#[allow(...)]` attributes. If a warning indicates dead code, remove it. If it flags a function as too long, refactor it. Silencing warnings hides real problems.

For genuine false positives, use `#[expect(..., reason = "...")]` instead of `#[allow]` so the suppression self-documents and warns if it becomes unnecessary.

## Python

Python is not in the devShell. Use `uv` for one-off scripts with dependencies:

```
uv run --with <packages> script.py
```

## Pull Requests

When creating a PR from a git worktree, the auto-generated branch name (e.g. `worktree-buzzing-sprouting-lighthouse`) is not suitable. Create a descriptive branch before pushing: `git checkout -b feat/my-feature` or `fix/the-bug`.

PR descriptions must follow the template in `.github/pull_request_template.md`.

When a PR touches the UI (Compose), attach before and after screenshots.

## Screenshots

Use the Android MCP for manual testing, debugging, and taking screenshots. Connect to the running emulator or device via `ConnectDevice`, then use `Snapshot` to capture the current screen.

## Roborazzi Screenshot Tests

Golden-file screenshot regression tests live in `app/src/test/java/.../ui/screenshot/`. They run on JVM via Robolectric (no emulator needed).

| Command | Purpose |
|---------|---------|
| `nix run .#record-screenshots` | Record golden PNGs via Docker (matches CI rendering) |
| `nix run .#verify-screenshots` | Verify current UI matches golden PNGs (fails on diff) |

When adding or changing UI components, re-record goldens with `nix run .#record-screenshots` and commit the updated PNGs.

**Important:** Goldens must be recorded in a Linux x86_64 environment to match CI. The `record-screenshots` command handles this automatically via Docker (`--platform linux/amd64`). On macOS it uses Rosetta for x86_64 emulation — requires Docker (e.g. Colima with `--vm-type vz --vz-rosetta`). Do NOT use `./gradlew recordRoborazziDebug` directly on macOS — the resulting PNGs will have subtle font rendering differences that fail CI verification.

## Native Rust Libraries

The app depends on a native Rust library (`libmybriefcase_bookmarks_ffi.so`). The Gradle `preBuild` task compiles it automatically via `cargo-ndk` — no manual steps needed. Requires the Nix dev shell for `cargo-ndk` on PATH; without it (e.g. CI lint/test), the task is skipped.

Rust sources live in `rust/ffi/`. The generated Kotlin bindings are at `app/src/main/java/uniffi/`.

## Project Context

See [README.md](README.md) for build prerequisites, launch parameters, and dev workflow. See [CONTEXT.md](CONTEXT.md) for domain language and architecture.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues on `johnspade/mybriefcase-bookmarks-android`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout — one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
