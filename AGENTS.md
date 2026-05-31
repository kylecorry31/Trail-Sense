Use the following scripts to work with the app, but do not modify them:
- `scripts/build.sh`: build the debug APK
- `scripts/unit-tests.sh [test-filter]`: run unit tests. The optional filter is forwarded to Gradle with `--tests`.
- `scripts/emulator-integration-tests.sh [test-class-or-method-filter] [timeout-seconds]`: run connected Android tests on the emulator with an optional timeout, defaulting to 1800 seconds. Fails if no emulator is connected. Most individual integration tests should finish in 60 to 180 seconds; use the timeout argument for focused runs when practical.
- `scripts/lint.sh`: run Detekt linting

Use the test filters whenever possible.