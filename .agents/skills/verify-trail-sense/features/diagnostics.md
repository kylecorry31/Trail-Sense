# Diagnostics

Use the Diagnostics tool to detect common issues that affect the tools in Trail Sense. If you have a problem with the app, run the Diagnostics tool to check whether it can find the cause. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_diagnostics.txt`](../../../../guides/en-US/guide_tool_diagnostics.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Diagnostics` → tap the tile.
- Precondition shortcut only: `$T open diagnostics`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Create a known issue: `$T perm revoke location` (kills the app).

- **Review.** Open the tool. After `perm revoke location` the app asks for the location permission on launch: deny it (`$T ui tap 'text=/Don.t allow/'`). The list then shows GPS rows `Permission not granted`, `Location not set` and `Overridden`.
- **Fix.** Grant it back and reopen. Run `$T perm grant location` then `$T open diagnostics`. The issue is gone.
- **Proof.** Snap with and without the issue.

## Gotchas

- Emulator-only rows appear even with all permissions: `Pedometer: Unavailable` (no step counter), `Battery: Battery usage restricted` and `Weather: Weather monitoring disabled`. No light-sensor row appears (the Light Meter tool is hidden). Don't mistake them for regressions.
