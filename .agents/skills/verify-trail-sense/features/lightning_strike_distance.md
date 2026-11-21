# Lightning Strike Distance

Use the Lightning Strike Distance tool to calculate how far away a lightning strike is and whether a storm is approaching or moving away. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_lightning_strike_distance.txt`](../../../../guides/en-US/guide_tool_lightning_strike_distance.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Lightning Strike Distance` → tap the tile.
- Precondition shortcut only: `$T open lightning-strike-distance`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Measure.** Tap at the flash, tap again at the thunder. Run `$T open lightning-strike-distance`, `$T ui tap id=start_btn` (the label reads `Lightning`, then `Thunder`), `sleep 3`, `$T ui tap id=start_btn`. `id=toolbar_title` shows the distance (about 0.95 mi after a nominal 3 s sleep on 2026-11-21).
- **Side effect.** Strikes are stored. Run `$T db query "select * from lightning"`. A row per measured strike.
- **Proof.** Snap the distance; save the db query output. The previous strike appears in `id=previous_strike` ("Last strike at …"); the UI shows no approaching/moving-away text.

## Gotchas

- Distance depends on wall-clock time between taps; add a known `sleep` between taps.
- Clean up with `$T db exec "delete from lightning"`.
