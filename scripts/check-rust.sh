#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

echo "==> cargo fmt --check"
cargo fmt --manifest-path rust/Cargo.toml -- --check

echo "==> cargo clippy"
cargo clippy --manifest-path rust/Cargo.toml -- -D warnings

echo "==> cargo test"
cargo test --manifest-path rust/Cargo.toml

echo "All Rust checks passed."
