# Temperature Estimation

Use the Temperature Estimation tool to estimate the temperature at a different elevation and to estimate how the temperature feels with wind and humidity. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_temperature_estimation.txt`](../../../../guides/en-US/guide_tool_temperature_estimation.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Temperature Estimation` → tap the tile.
- Precondition shortcut only: `$T open temperature-estimation`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Elevation.** Enter a base temperature and elevations per the guide. Run `$T ui find Temperature`, then `$T ui type <value> id=<reported id>` per field. Estimated temperature shown.
- **Feels like.** Enter wind and humidity. Run per the guide. Feels-like value shown.
- **Proof.** Snap each result; compare with the guide's examples.

## Gotchas

- Temperature units follow the settings.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
