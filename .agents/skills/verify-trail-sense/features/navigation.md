# Navigation

Use the Navigation tool to navigate to a beacon, follow a path, or follow a bearing. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_navigation.txt`](../../../../guides/en-US/guide_tool_navigation.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Navigation` → tap the tile.
- Bottom navigation tab `Navigation` (default tabs).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open navigation`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T env geo 42.3601 -71.0589 20`.
- A beacon (create one per beacons.md).

- **Compass.** Tap the Navigation tab. Run `$T ui tap desc=Navigation`. Compass with bearing.
- **Heading.** Rotate the device. Run `$T env sensor set magnetic-field 0 20 -40` then `20 0 -40`. Bearing text changes.
- **Navigate to beacon.** Pick the beacon per Navigate to a beacon. Run `$T ui find Beacon`. Distance and direction to the beacon.
- **Proof.** Snap compass at two headings and while navigating; distance matches the beacon's coordinates.

## Gotchas

- Bearing depends on accelerometer and magnetometer together; set acceleration upright (`0 9.81 0`) or flat consistently.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
