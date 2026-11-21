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

- **Time a drop.** Start the timer, wait, stop it. Dismiss the `Disclaimer` dialog on first open (`text=/^OK$/`). The control is an icon tile with no text or id in the layout dump, so tap its center with `$T ui tapxy 360 700` (a quick tap starts, a second tap after a `sleep` stops). The height appears in `id=toolbar_title` (72.76 ft after a few seconds on 2026-11-21).
- **Proof.** Snap the result; the height grows with the measured time (≈ g·t²/2).

## Gotchas

- The tool is disabled by default for real users; seed enables it.
