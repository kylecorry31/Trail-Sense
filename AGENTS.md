Use the following scripts to work with the app, but do not modify them:
- `scripts/build.sh`: build the debug APK
- `scripts/run-unit-tests.sh [test-filter]`: run unit tests. The optional filter is forwarded to Gradle with `--tests`.
- `scripts/run-emulator-integration-tests.sh [test-class-or-method-filter] [timeout-seconds]`: run connected Android tests on the emulator with an optional timeout, defaulting to 1800 seconds. Fails if no emulator is connected. Most individual integration tests should finish in 60 to 180 seconds; use the timeout argument for focused runs when practical.
- `scripts/lint.sh`: run Detekt linting
- If performing a code review, ignore the non en-US translations (strings, guides, and changelogs), the guide files outside of guides/en-US (site and app guides are copied from that on demand and do not need review), and the agent skills.

Use the test filters whenever possible.
