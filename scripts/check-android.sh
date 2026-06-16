#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

echo "==> gradlew lint"
./gradlew lint

echo "==> gradlew testDebugUnitTest"
./gradlew testDebugUnitTest

echo "All Android checks passed."
