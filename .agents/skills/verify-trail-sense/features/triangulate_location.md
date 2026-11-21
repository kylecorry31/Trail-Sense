# Triangulate Location

Use the Triangulate Location tool to find the coordinates of a location from two known locations. You can use it to find your own location if your GPS isn't working, or to find the coordinates of a location in the distance. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_triangulate_location.txt`](../../../../guides/en-US/guide_tool_triangulate_location.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Triangulate Location` → tap the tile.
- Precondition shortcut only: `$T open triangulate-location`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Two beacons or known points (see beacons.md).

- **Find location.** Enter two locations and bearings per Find your location. Run `$T open triangulate-location`. Triangulated coordinate and map shown.
- **Proof.** Snap the result; check the coordinate against a hand calculation.

## Gotchas

- Bearings that are nearly parallel give no or unstable results by design.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
