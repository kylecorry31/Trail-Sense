#!/usr/bin/env bash

set -euo pipefail

package="${1:-com.kylecorry.trail_sense}"

device_date="$(adb shell date +%m-%d | tr -d '\r')"

matches="$({
    adb logcat -b all -d -v threadtime -T "${device_date} 00:00:00.000" |
        grep -i 'AudioHardening' |
        grep -F "$package"
} || true)"

if [[ -z "$matches" ]]; then
    echo "No AudioHardening events found for $package in today's retained logs."
    exit 0
fi

echo "$matches"
