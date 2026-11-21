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

- **Review.** Open the tool. Run `$T open diagnostics`. Lists a location permission issue.
- **Fix.** Grant it back and reopen. Run `$T perm grant location` then `$T open diagnostics`. The issue is gone.
- **Proof.** Snap with and without the issue.

## Gotchas

- Emulator-only issues (no step counter, no light sensor) always appear; don't mistake them for regressions.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
