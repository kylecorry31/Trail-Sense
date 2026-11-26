**Always use the `verify-trail-sense` skill and its `tsctl` script for every device or emulator interaction.** Load the skill before touching a device. Never call `adb`, `am`, `dumpsys`, the `android` CLI, or `gradlew` directly for building, installing, driving, or inspecting the app. `tsctl` enforces the emulator-versus-physical-device safety gates that raw commands skip. If `tsctl` can't do what you need, add a subcommand to it (and its docs) instead of working around it with raw commands. Behavior changes to the app are not verified until you have driven the real user path with `tsctl` on the emulator and checked `tsctl logs --crash` is empty.

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

## Comments

Do not write comments that say what code does. Make the code say it with names, types, and structure. The only allowed comment explains why some confusing or unclear code is needed, usually because of a behavior of Android, a device, a library, or a file format that we can't change. A rename or refactor can make a surprise in our own code obvious, so prefer that over a comment. Do not leave commented-out code, TODOs, or section banners.

Start every allowed comment with `WHY:`, for example `// WHY: Some Samsung devices report X as Y`. Keep it short. It may end with a link to the issue that explains the constraint. The Orion `WhyComment` detekt rule rejects any `//` or `/* */` comment that doesn't start with `WHY:`.

KDoc (`/** */`) is allowed, but only when a parameter, return value, or thrown exception is unclear from its name and type, for example its units, range, coordinate system, what null or a sentinel value means, a precondition, or which exceptions it throws. Do not write KDoc that restates the name of the function or its parameters.
