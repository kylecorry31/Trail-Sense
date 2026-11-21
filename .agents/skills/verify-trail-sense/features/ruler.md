# Ruler

Use the Ruler tool to measure small objects or distances on a map. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_ruler.txt`](../../../../guides/en-US/guide_tool_ruler.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Ruler` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open ruler`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Measure.** Open the tool and tap a position on the ruler. Run `$T open ruler`, `$T ui tapxy 120 700`. The touch strip is only the left ~80 dp of the screen, so `tapxy 360 …` registers nothing. `id=measurement` then reads e.g. `1.9875 in`.
- **Units.** Change units per the guide. Run `$T ui tap id=ruler_unit_btn` (its text toggles `in`/`cm`). Value switches units.
- **Proof.** Snap both units.

## Gotchas

- Physical accuracy depends on the emulator's reported DPI; only check behavior, not real-world length.
- After `seed` (feet/miles) the ruler starts in inches.
