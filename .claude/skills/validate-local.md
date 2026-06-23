# validate-local

Run local CI validation. Use when the user types `/validate-local`.

## Instructions

Run the project's validation script via Nix. Do NOT run individual steps (clippy, lint, test) separately — the `validate` wrapper handles ordering and error propagation.

**Default (fast, pre-commit):**

```bash
nix run .#validate
```

**Full CI equivalent** (if the user says "full", "all", or "screenshots"):

```bash
nix run .#validate-all
```

### Rules

1. Always use `nix run .#validate` or `nix run .#validate-all` — never invoke `gradle-lint`, `gradle-test`, `nix flake check`, or `cargo` directly.
2. Do NOT `cd` anywhere — run from the repo root (the current working directory).
3. Let the command run to completion (it may take several minutes). Use a 10-minute timeout.
4. Report the result: if it exits 0, say validation passed. If non-zero, show the failing output and identify what failed.
5. Do not attempt to fix failures automatically unless the user asks.
