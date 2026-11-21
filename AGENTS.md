Use `.agents/skills/verify-trail-sense/scripts/tsctl` to build, test, lint, and run the app (see the `verify-trail-sense` skill and `tsctl --help`):
- `tsctl build [variant...]`: build APKs (default `debug`)
- `tsctl test unit [test-filter]`: run unit tests. The optional filter is forwarded to Gradle with `--tests`.
- `tsctl test android [test-class-or-method-filter] [--timeout seconds]`: run connected Android tests on the emulator, with a timeout defaulting to 1800 seconds. Most individual integration tests should finish in 60 to 180 seconds; use the timeout for focused runs when practical.
- `tsctl lint`: run Detekt linting
- `tsctl emu start`, `tsctl app install`, `tsctl app seed`, `tsctl open <tool>`, `tsctl ui ...`: start the emulator and drive the app to verify changes

Use the test filters whenever possible.

tsctl defaults to the emulator. It needs explicit user approval for any action on a physical device, unless the user gave full control of that package on that device.

If performing a code review, ignore the non en-US translations (strings, guides, and changelogs), the guide files outside of guides/en-US (site and app guides are copied from that on demand and do not need review), and the agent skills.

The `guides/en-US/*.txt` files are the source of truth for the user guides. All other guides (other locales, and the copies in the site and app) are only updated by the translation or weekly update process (`scripts/weekly-update.sh`, run manually by the user), so never edit them.

Always use the android CLI and associated skills when applicable.
