# Beacons

Use the Beacons tool to save locations and navigate back to them later. The procedures are in the user guide [`guides/en-US/guide_tool_beacons.txt`](../../../../guides/en-US/guide_tool_beacons.txt). This file adds how to drive and prove them. The create journey below was driven end to end on 2026-11-21 (API 36 emulator).

## Sub-features

Every section of [`guide_tool_beacons.txt`](../../../../guides/en-US/guide_tool_beacons.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified. Proven so far: Create a beacon (at the GPS location, from both entry points). Create a beacon from a QR code needs a camera image, so it can't be verified on the emulator.

## How to get to it (user POV)

- Tools tab → search `Beacons` → tap the tile (proven).
- Navigation tab → beacon button (see navigation.md).
- Quick action "Create beacon" (see the guide).
- A `geo:` link from another app opens the create form. MainActivity handles `android.intent.action.VIEW` with the `geo` scheme.
- Precondition shortcut only: `$T open beacons` (tool id 7, proven).

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- `$T env geo 42.3601 -71.0589 20` sets the GPS fix.
- No beacon named `Verify Camp`. Check with `$T db query "select name from beacons"`.

- **Open from Tools.** Tap the Tools tab, search, and tap the tile. Run `$T ui tap id=action_experimental_tools`, `$T ui type Beacons id=search_view_edit_text`, and `$T ui tap 'text=/^Beacons$/,id=title'`. The toolbar title reads `Beacons`. A fresh install shows `No beacons` (`id=beacon_empty_text`).
- **Open the create menu.** Tap the floating button. Run `$T ui tap id=create_btn`. A speed-dial menu appears with `QR Code`, `Import GPX`, `Group`, and `Beacon`.
- **Choose Beacon.** Run `$T ui tap 'text=/^Beacon$/'`. The toolbar reads `Create beacon`.
- **Name.** Run `$T ui type "Verify Camp" 'id=/:id\/text_input$/'`. The plain `id=text_input` selector is ambiguous because it also matches `text_input_holder`.
- **Location from GPS.** Tap the location's pin button, pick `GPS`, then `OK`. Run `$T ui tap id=gps_btn --first`, `$T ui tap 'text=/^GPS$/'`, and `$T ui tap 'text=/^OK$/' --wait 3000`. Location (`id=utm`) shows `42.360100°, -71.058898°` and Elevation (`id=amount`) shows `159` (ft).
- **Save.** Tap the checkmark. Run `$T ui tap id=toolbar_right_button`, then `$T ui wait "Verify Camp"`. The list shows `Verify Camp` (`id=title`).
- **Side effect.** Run `$T db query "select _id, name, latitude, longitude, elevation from beacons"`. You get one row: `Verify Camp | 42.3601 | -71.058898 | ~48.5` (elevation is stored in meters).
- **Proof.** Run `$T ui snap beacons/01-open`, `$T ui snap beacons/02-form-filled`, and `$T ui snap beacons/03-list`, and save the db query to `captures/verify/$TSCTL_RUN/beacons/04-db.txt`. Finally, `$T logs --crash` is empty.

## Gotchas

- `text=Beacon` matches `Beacons` and `Create beacon` too (it's a case-insensitive substring). Use `text=/^Beacon$/`.
- The form has two `gps_btn` buttons (location and elevation), so pass `--first` for the location one.
- Elevation is converted from the GPS (ellipsoid) altitude to sea level with seed's GPS altimeter mode: `env geo ... 20` in Boston gives 159 ft in the form and ~48.5 m in the db (geoid offset ≈ +28 m). Assert on that, not on the raw `geo` altitude. Other altimeter modes give other values.
- Clean up with `$T db exec "delete from beacons where name='Verify Camp'"` or delete through the UI. Keep the captures.
