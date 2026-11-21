# Signal Finder

Use the Signal Finder tool to find a place where you have cell signal. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_signal_finder.txt`](../../../../guides/en-US/guide_tool_signal_finder.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Signal Finder` → tap the tile.
- Precondition shortcut only: `$T open signal-finder`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Location permission.

- **Signals.** Open the tool. Run `$T open signal-finder`. Detected cell signals (the emulator simulates one network).
- **Proof.** Snap the list. Nearby tower data is bundled; check that towers near the `geo` fix are listed.

## Gotchas

- Change emulated signal strength with `$T adb -- emu gsm signal-profile 1..4`; real towers can't be observed.
- Don't place emergency calls.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
