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

- **Measure.** Open the tool and tap a position on the ruler. Run `$T open ruler`, `$T ui tapxy 360 600`. The measured length is shown.
- **Units.** Change units per the guide. Run `$T ui find` the units toggle. Value switches units.
- **Proof.** Snap both units.

## Gotchas

- Physical accuracy depends on the emulator's reported DPI; only check behavior, not real-world length.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
