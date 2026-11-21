# Ballistics

Use the Ballistics tool for firearm calculators that help hunters make a shot. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_ballistics.txt`](../../../../guides/en-US/guide_tool_ballistics.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Ballistics` → tap the tile.
- Precondition shortcut only: `$T open ballistics`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Calculator.** Pick a calculator (sight in, bullet drop, energy) and enter values from the guide. Run `$T ui find "Bullet drop"` to list the fields, then `$T ui type 100 id=<reported id>` for each. The result text updates.
- **Proof.** Snap the filled form with the computed result; compare it with a hand calculation or the unit tests' expected values.

## Gotchas

- Pure calculator: no sensors or permissions. Number fields reject text, so type digits only.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
