#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

cleanup() {
  adb shell am force-stop com.kylecorry.trail_sense.test >/dev/null 2>&1 || true
  adb shell am force-stop com.kylecorry.trail_sense.staging >/dev/null 2>&1 || true
}

trap cleanup EXIT

./scripts/run-staging-smoke-test.sh
