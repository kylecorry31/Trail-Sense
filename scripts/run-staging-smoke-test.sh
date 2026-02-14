#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

log_file="app/emulator.log"
adb logcat -c
touch "$log_file"
chmod 777 "$log_file"
adb logcat >> "$log_file" &
logcat_pid=$!

cleanup() {
  kill "$logcat_pid" 2>/dev/null || true
}

trap cleanup EXIT

./gradlew installStaging

./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.kylecorry.trail_sense.SmokeTest#smokeTest
