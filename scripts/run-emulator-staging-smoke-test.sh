#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

gps_pid=""

cleanup() {
  adb shell am force-stop com.kylecorry.trail_sense.test >/dev/null 2>&1 || true
  adb shell am force-stop com.kylecorry.trail_sense.staging >/dev/null 2>&1 || true
  if [ -n "$gps_pid" ]; then
    kill "$gps_pid" 2>/dev/null || true
    wait "$gps_pid" 2>/dev/null || true
  fi
}

trap cleanup EXIT

./scripts/run-emulator-gps.sh &
gps_pid=$!
./scripts/run-staging-smoke-test.sh
