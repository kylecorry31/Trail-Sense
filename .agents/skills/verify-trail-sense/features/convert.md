# Convert

Use the Convert tool to convert between coordinate systems and between units of distance, temperature, volume, weight, and time. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_convert.txt`](../../../../guides/en-US/guide_tool_convert.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Convert` → tap the tile.
- Quick action (see the guide's Quick action section).
- Precondition shortcut only: `$T open convert`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.

- **Coordinates.** Enter a coordinate and pick a target format. Run `$T ui type "42.3601, -71.0589" id=utm` (the "Location" hint is on its holder). The `to_units` spinner picks the target format and `id=result` shows it. Converted coordinate shown (e.g. UTM).
- **Units.** Switch to units, convert 1 mile to km. Tap the `Distance` tab (`$T ui tap 'text=Distance'`), then `$T ui type 1 'id=/:id\/unit_edit$/'`. With the default From `Meters` and To `Feet`, `id=result` reads `3.2808 ft` (driven on 2026-11-21). Spinners are `from_units` and `to_units`; the swap button is `swap_btn`. Picking Miles and Kilometers was not confirmed (the dropdown rows are all `id=item_name`).
- **Proof.** Snap each converted result.

## Gotchas

- Tabs switch between coordinate and unit converters; there is no toolbar; the tabs are `id=tabs` (Coordinates, Distance, Temperature, Volume, Weight, Time).
