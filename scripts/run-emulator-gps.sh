#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -gt 4 ]; then
  echo "Usage: $0 [longitude] [latitude] [altitude] [interval-seconds]" >&2
  exit 2
fi

longitude="${1:--72.0}"
latitude="${2:-42.0}"
altitude="${3:-100}"
interval_seconds="${4:-5}"

if ! [[ "$interval_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "Interval must be a positive whole number of seconds." >&2
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

cleanup() {
  exit 0
}

trap cleanup INT TERM

while true; do
  adb -s "$selected_device" emu geo fix "$longitude" "$latitude" "$altitude" >/dev/null
  sleep "$interval_seconds"
done
