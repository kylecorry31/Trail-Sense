#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

if [ "$#" -gt 1 ]; then
  echo "Usage: $0 [test-filter]" >&2
  exit 2
fi

args=(testDebugUnitTest)

if [ "$#" -eq 1 ]; then
  args+=(--tests "$1")
fi

./gradlew "${args[@]}"
