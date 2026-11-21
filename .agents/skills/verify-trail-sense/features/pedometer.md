# Pedometer

Use the Pedometer tool to track the distance you travel by counting your steps. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_pedometer.txt`](../../../../guides/en-US/guide_tool_pedometer.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Pedometer` → tap the tile.
- Quick action (see the guide's Quick action section).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open pedometer`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- A step counter sensor. The emulator has none, so the tool is unavailable.

- **Availability.** Look for the tool in the Tools tab. Run `$T ui tap id=action_experimental_tools`, `$T ui type Pedometer id=search_view_edit_text`. Not listed on the emulator.
- **Proof.** Only the absence can be proven on the emulator. Real counting needs a physical device (with user approval) or the instrumented tests.

## Gotchas

- Activity recognition permission is also required on a device.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
