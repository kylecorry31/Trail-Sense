#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

./gradlew installStaging

./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.kylecorry.trail_sense.SmokeTest#smokeTest
