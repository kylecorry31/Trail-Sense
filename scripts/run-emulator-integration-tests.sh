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

gradle_args=(connectedDebugAndroidTest)
if [ "$#" -ge 1 ] && [ -n "$1" ]; then
  gradle_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$1")
fi

ANDROID_SERIAL="$selected_device" timeout --foreground "${timeout_seconds}s" ./gradlew "${gradle_args[@]}"
