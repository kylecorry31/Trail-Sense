# QR Code Scanner

Use the QR Code Scanner tool to scan QR codes. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_qr_code_scanner.txt`](../../../../guides/en-US/guide_tool_qr_code_scanner.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `QR Code Scanner` → tap the tile.
- Precondition shortcut only: `$T open qr-code-scanner`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T perm grant camera`.

- **Scanner.** Open the tool. Run `$T open qr-code-scanner`. Camera preview runs.
- **Share via QR.** Share a beacon or note as a QR code (see the guide's Share data section). Run from beacons.md or notes.md. A QR code is displayed.
- **Proof.** Snap the displayed QR. Scanning needs a real camera aimed at a code: report as not verified on the emulator.

## Gotchas

- The emulated camera scene contains no QR codes.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
