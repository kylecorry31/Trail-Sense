Use the following scripts to work with the app, but do not modify them:
- `scripts/build.sh`: build the debug APK
- `scripts/unit-tests.sh [test-filter]`: run unit tests. The optional filter is forwarded to Gradle with `--tests`.
- `scripts/emulator-integration-tests.sh [test-class-or-method-filter]`: run connected Android tests on the emulator. Fails if no emulator is connected.
- `scripts/lint.sh`: run Detekt linting
