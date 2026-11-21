# Clouds

Use the Clouds tool to identify clouds and log them to help predict the weather. If your phone has a barometer, Trail Sense combines your cloud observations with barometer readings to improve the weather prediction in the Weather tool. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_clouds.txt`](../../../../guides/en-US/guide_tool_clouds.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Clouds` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open clouds`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Log manually.** Create a log entry and pick a cloud type per Identify a cloud manually. Run `$T ui tap id=add_btn`, then `$T ui tap 'text=/^Manual$/'` (the speed dial offers File, Camera, Manual). The results screen (`id=cloud_list`) lists the cloud types. Tapping a row's text opens an info dialog (dismiss with `text=/^OK$/`); select a cloud with its checkbox, then save with `$T ui tap id=toolbar_right_button`. The entry appears in the cloud log. The list screen and the Manual results screen were driven on 2026-11-21; the save was not confirmed.
- **Side effect.** Read the stored log. Run `$T db query "select * from clouds"`. A row with the chosen cloud.
- **Proof.** Snap the log list and save the db query output.

## Gotchas

- Automatic identification uses the camera; the emulated scene isn't a sky, so report that path as not verified.
