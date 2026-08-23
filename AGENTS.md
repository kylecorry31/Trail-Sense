Use the following scripts to work with the app, but do not modify them:
- `scripts/build.sh`: build the debug APK
- `scripts/run-unit-tests.sh [test-filter]`: run unit tests. The optional filter is forwarded to Gradle with `--tests`.
- `scripts/run-emulator-integration-tests.sh [test-class-or-method-filter] [timeout-seconds]`: run connected Android tests on the emulator with an optional timeout, defaulting to 1800 seconds. Fails if no emulator is connected. Most individual integration tests should finish in 60 to 180 seconds; use the timeout argument for focused runs when practical.
- `scripts/lint.sh`: run Detekt linting

Use the test filters whenever possible.

If a substantial amount of code or a whole feature was implemented by you (an agent), please create a TODO.md file in the repo root with the text "A substantial portion of this code was developed with AI. A human must review, clean up, and manually test this code before submitting a pull request."