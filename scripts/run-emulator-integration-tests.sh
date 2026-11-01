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

gradle_args=(smallPhoneApi36DebugAndroidTest)
if [ "$#" -ge 1 ] && [ -n "$1" ]; then
  gradle_args+=("-Pandroid.testInstrumentationRunnerArguments.class=$1")
fi

timeout --foreground "${timeout_seconds}s" ./gradlew "${gradle_args[@]}"
