# Cliff Height

Use the Cliff Height tool to estimate the height of a cliff by timing a falling object. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_cliff_height.txt`](../../../../guides/en-US/guide_tool_cliff_height.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Cliff Height` → tap the tile.
- Precondition shortcut only: `$T open cliff-height`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `@pref_cliff_height_enabled` true (`seed` sets it; the tool is hidden otherwise).

- **Time a drop.** Start the timer, wait, stop it. Run `$T ui find Start`, tap it, wait, tap again. Height estimate appears.
- **Proof.** Snap the result; the height grows with the measured time (≈ g·t²/2).

## Gotchas

- The tool is disabled by default for real users; seed enables it.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
