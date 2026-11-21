# Climate

Use the Climate tool to view historic climate data for a location, so you know what conditions to expect. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_climate.txt`](../../../../guides/en-US/guide_tool_climate.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Climate` → tap the tile.
- Precondition shortcut only: `$T open climate`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T env geo 42.3601 -71.0589 20`.

- **Chart.** Open the tool. Run `$T open climate`. Temperature chart and climate classification for the location.
- **Change location.** Change the location per the guide. Run `$T ui find Location`. Values change for the new location.
- **Proof.** Snap before and after changing location or date.

## Gotchas

- Uses bundled historic data; values are coarse by design.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
