# Light Meter

Use the Light Meter tool to measure a flashlight's beam distance, which is how far the flashlight projects usable light. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_light_meter.txt`](../../../../guides/en-US/guide_tool_light_meter.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Light Meter` → tap the tile.
- Precondition shortcut only: `$T open light-meter`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- A light sensor. The emulator has none, so the tool is unavailable.

- **Availability.** Look for the tool in the Tools tab. Run `$T ui tap id=action_experimental_tools`, `$T ui type "Light Meter" id=search_view_edit_text`. Not listed on the emulator.
- **Proof.** On the emulator only the absence can be proven. Beam distance needs a device with a light sensor (with user approval).

## Gotchas

- `sensor set light` is accepted by the console but the API 36 image exposes no light sensor to apps.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
