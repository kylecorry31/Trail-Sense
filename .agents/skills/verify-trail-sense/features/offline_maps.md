# Offline Maps

Use the Offline Maps tool to import and manage maps that you can use without an Internet connection. The tool supports images, PDFs, and Mapsforge (.map) files. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_offline_maps.txt`](../../../../guides/en-US/guide_tool_offline_maps.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Offline Maps` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open offline-maps`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- A map file on the device: `$T adb -- push some-map.jpg /sdcard/Download/`.

- **Import.** Import a map per Import a map. Run `$T ui tap id=create_btn` then pick the file in the system picker. The map appears in the list.
- **Calibrate.** Calibrate a photo map per the guide. Run follow the guide's calibration steps. Map shows calibrated status.
- **Side effect.** Stored maps. Run `$T db query "select * from maps"`. A row for the imported map.
- **Proof.** Snap the list and the calibrated map; save the db output.

## Gotchas

- The file picker is the system DocumentsUI, so selectors come from it, not the app.
- Mapsforge .map files go to `offline_map_files`, photo maps to `maps`.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
