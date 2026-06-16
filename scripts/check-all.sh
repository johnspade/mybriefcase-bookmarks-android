#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

echo "==============================="
echo " Rust checks"
echo "==============================="
scripts/check-rust.sh

echo ""
echo "==============================="
echo " Android checks"
echo "==============================="
scripts/check-android.sh

echo ""
echo "All checks passed."
