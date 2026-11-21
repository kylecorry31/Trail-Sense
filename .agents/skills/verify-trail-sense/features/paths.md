# Paths

Use the Paths tool to record waypoints while you hike and to follow a trail. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_paths.txt`](../../../../guides/en-US/guide_tool_paths.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Paths` → tap the tile.
- Quick action (see the guide's Quick action section).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open paths`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Location permission and `$T env geo ...`. Background location is granted by `seed`.

- **Backtrack.** Start Backtrack per Start and stop Backtrack. Run `$T open paths`, `$T ui find Backtrack`. Backtrack status shows running; a notification appears.
- **Move.** Walk a route. Run `$T env geo 42.3601 -71.0589`, wait, `$T env geo 42.3611 -71.0579`, .... Points accumulate after each recording interval.
- **Side effect.** Stored points. Run `$T db query "select count(*) from waypoints"`. Count grows.
- **Proof.** Snap the path view; save the db output. Stop Backtrack afterwards.

## Gotchas

- The recording interval defaults to minutes; shorten it in the Paths settings first, or the run takes long.
- `scripts/debugging/list_backtrack_points.py` and the trail-sense-backtrack-session-summary skill read logs for longer sessions.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
