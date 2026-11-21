# Solar Panel Aligner

Use the Solar Panel Aligner tool to position a solar panel for the most power while you camp. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_solar_panel_aligner.txt`](../../../../guides/en-US/guide_tool_solar_panel_aligner.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Solar Panel Aligner` → tap the tile.
- Precondition shortcut only: `$T open solar-panel-aligner`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T geo 42.3601 -71.0589 20`.

- **Align.** Open the tool. Run `$T open solar-panel-aligner`. Target azimuth and tilt for the panel.
- **Move.** Change the heading. Run `$T sensor set magnetic-field 0 20 -40`. Direction hints update.
- **Proof.** Snap the aligner at two headings.

## Gotchas

- Results depend on the emulator's time of day.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
