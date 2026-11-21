# Convert

Use the Convert tool to convert between coordinate systems and between units of distance, temperature, volume, weight, and time. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_convert.txt`](../../../../guides/en-US/guide_tool_convert.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Convert` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open convert`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Coordinates.** Enter a coordinate and pick a target format. Run `$T ui find Location`, then `$T ui type "42.3601, -71.0589" id=<reported id>`. Converted coordinate shown (e.g. UTM).
- **Units.** Switch to units, convert 1 mile to km. Run `$T ui find Distance`. 1.609 km.
- **Proof.** Snap each converted result.

## Gotchas

- Tabs switch between coordinate and unit converters; check the toolbar title before typing.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
