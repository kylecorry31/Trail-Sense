#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

if [ "$#" -gt 2 ]; then
  echo "Usage: $0 [test-class-or-method-filter] [timeout-seconds]" >&2
  exit 2
fi

timeout_seconds="${2:-1800}"

if ! [[ "$timeout_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "Timeout must be a positive whole number of seconds." >&2
  exit 2
fi

mapfile -t devices < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')

if [ "${#devices[@]}" -eq 0 ]; then
  echo "No connected Android devices found." >&2
  exit 1
fi

selected_device=""
for device in "${devices[@]}"; do
  is_qemu="$(adb -s "$device" shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')"
  if [ "$is_qemu" = "1" ]; then
    selected_device="$device"
    break
  fi
done

if [ -z "$selected_device" ]; then
  echo "No connected emulator found." >&2
  exit 1
fi

gradle_args=(assembleDebug assembleDebugAndroidTest)

ANDROID_SERIAL="$selected_device" ./gradlew "${gradle_args[@]}"

test_package="com.kylecorry.trail_sense.test"
test_runner="$test_package/androidx.test.runner.AndroidJUnitRunner"
app_apk="app/build/outputs/apk/debug/app-debug.apk"
test_apk="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

adb -s "$selected_device" install --no-streaming -r -t "$app_apk"
adb -s "$selected_device" install --no-streaming -r -t "$test_apk"

instrumentation_args=(-w -r)
if [ "$#" -ge 1 ]; then
  instrumentation_args+=(-e class "$1")
fi

results_file="$(mktemp)"
trap 'rm -f "$results_file"' EXIT

set +e
timeout "${timeout_seconds}s" adb -s "$selected_device" shell am instrument \
  "${instrumentation_args[@]}" "$test_runner" | tee "$results_file"
instrumentation_status="${PIPESTATUS[0]}"
set -e

if [ "$instrumentation_status" -ne 0 ]; then
  exit "$instrumentation_status"
fi

if grep -qE 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_RESULT: shortMsg=' "$results_file"; then
  exit 1
fi
