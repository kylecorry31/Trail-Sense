# Trail Sense verification map

This directory is the maintained source for verifying Trail Sense's user-facing behavior. It has one file per user guide in `guides/en-US/guide_tool_*.txt`. Each file links its guide for the step-by-step procedure, and adds preconditions, tsctl commands, stable handles, and what proves the feature works.

## Baseline preconditions

- `T=.agents/skills/verify-trail-sense/scripts/tsctl` from the repo root.
- The emulator is running (`$T emu start`), the current build is installed (`$T app install`), and the app is seeded (`$T app seed --clear` for an empty data dir).
- `$T doctor` ends with `doctor: OK`.
- `export TSCTL_RUN=<feature>-$(date +%Y%m%d-%H%M%S)` so evidence lands in `captures/verify/$TSCTL_RUN/`.
- Drive only the emulator unless the user approved a specific action on a device (see SKILL.md).

## Driving conventions

- Reach the tool the way a user does: the Tools tab search, or a bottom-nav tab. `$T open <tool>` only sets up preconditions.
- Prefer `id=` handles (`toolbar_title`, `toolbar_right_button`, `create_btn`, `search_view_edit_text`, `title`) and exact-text regexes (`text=/^OK$/`) over coordinates.
- Inject the environment with `$T geo` and `$T sensor set`. Set state with `$T pref`/`$T db exec` only as preconditions.
- After a mutation, put the data back (`db exec` delete, or `app restore <backup>` with the directory tsctl printed). Proof artifacts stay.

## Proof and skip reporting

- UI proof: `$T ui snap <feature>/<step>` before and after each action (screenshot + layout JSON).
- Side-effect proof: `$T db query` or `$T pref get` output saved next to the snaps.
- `$T logs --crash` is empty at the end of the run.
- When the emulator can't meet a precondition (no step counter, light sensor, torch, real camera image, or audio), report that journey as not verified with the reason. Don't verify it through a weaker path.

## Feature entry contract

Each file has an H1 title and a paragraph that links its guide, then exactly four H2s in this order: `Sub-features` (a link to the guide, whose section headings are the feature ids, plus anything proven or unverifiable), `How to get to it (user POV)`, `Driving it with tsctl` (starts with `Preconditions:`, then labeled action → command → observable result bullets, ending with **Proof**), and `Gotchas`. Steps that say "verify with `$T ui find`" come from the guide, not from a driven run. Confirm them and update the file on first use.

## Features

| Feature | File | `tsctl open` | Emulator | Proven |
| --- | --- | --- | --- | --- |
| Astronomy | [astronomy.md](./astronomy.md) | `astronomy` | yes | partly (title, date arrows) |
| Augmented Reality | [augmented_reality.md](./augmented_reality.md) | `augmented-reality` | yes | no |
| Ballistics | [ballistics.md](./ballistics.md) | `ballistics` | yes | no |
| Battery | [battery.md](./battery.md) | `battery` | yes | yes (power saving switch) |
| Beacons | [beacons.md](./beacons.md) | `beacons` | yes | yes (create, both entry points) |
| Bubble Level | [bubble_level.md](./bubble_level.md) | `bubble-level` | yes | yes (flat and tilted) |
| Cliff Height | [cliff_height.md](./cliff_height.md) | `cliff-height` | yes | yes (timed drop) |
| Climate | [climate.md](./climate.md) | `climate` | yes | yes (zone, temperature text) |
| Clinometer | [clinometer.md](./clinometer.md) | `clinometer` | yes | yes (angle and slope) |
| Clock | [clock.md](./clock.md) | `clock` | yes | yes (GPS time, offset) |
| Clouds | [clouds.md](./clouds.md) | `clouds` | yes | partly (manual results) |
| Convert | [convert.md](./convert.md) | `convert` | yes | partly (distance default) |
| Declination | [declination.md](./declination.md) | `declination` | yes | yes (Boston and Sydney) |
| Diagnostics | [diagnostics.md](./diagnostics.md) | `diagnostics` | yes | yes (permission rows) |
| Field Guide | [field_guide.md](./field_guide.md) | `field-guide` | yes | partly (categories, create page) |
| Flashlight | [flashlight.md](./flashlight.md) | `flashlight` | yes | partly (screen, torch toggle) |
| Light Meter | [light_meter.md](./light_meter.md) | `light-meter` | no | no |
| Lightning Strike Distance | [lightning_strike_distance.md](./lightning_strike_distance.md) | `lightning-strike-distance` | yes | yes (measure, db row) |
| Magnifier | [magnifier.md](./magnifier.md) | `magnifier` | yes | no |
| Map | [map.md](./map.md) | `map` | yes | partly (menu, long press, layers) |
| Map Layers | [map_layers.md](./map_layers.md) | `map` | yes | partly (sheet opens) |
| Metal Detector | [metal_detector.md](./metal_detector.md) | `metal-detector` | yes | yes (baseline and raised field) |
| Mirror Camera | [mirror_camera.md](./mirror_camera.md) | `mirror-camera` | yes | no |
| Navigation | [navigation.md](./navigation.md) | `navigation` | yes | no |
| Notes | [notes.md](./notes.md) | `notes` | yes | yes (create, QR/Delete menu, db) |
| Offline Maps | [offline_maps.md](./offline_maps.md) | `offline-maps` | yes | partly (add menu) |
| Packing Lists | [packing_lists.md](./packing_lists.md) | `packing-lists` | yes | partly (list screen) |
| Paths | [paths.md](./paths.md) | `paths` | yes | no |
| Pedometer | [pedometer.md](./pedometer.md) | `pedometer` | no | no |
| QR Code Scanner | [qr_code_scanner.md](./qr_code_scanner.md) | `qr-code-scanner` | yes | no |
| Ruler | [ruler.md](./ruler.md) | `ruler` | yes | yes (tap, units) |
| Sensors | [sensors.md](./sensors.md) | `sensors` | yes | yes (barometer card) |
| Settings | [settings.md](./settings.md) | `settings` | yes | no |
| Signal Finder | [signal_finder.md](./signal_finder.md) | `signal-finder` | yes | partly (towers listed) |
| Solar Panel Aligner | [solar_panel_aligner.md](./solar_panel_aligner.md) | `solar-panel-aligner` | yes | partly (dialog, fields) |
| Survival Guide | [survival_guide.md](./survival_guide.md) | `survival-guide` | yes | partly (search) |
| Temperature Estimation | [temperature_estimation.md](./temperature_estimation.md) | `temperature-estimation` | yes | no |
| Tides | [tides.md](./tides.md) | `tides` | yes | yes (create table, db) |
| Tools screen | [tools.md](./tools.md) | - | yes | partly (open a tool) |
| Triangulate Location | [triangulate_location.md](./triangulate_location.md) | `triangulate-location` | yes | no |
| Turn Back | [turn_back.md](./turn_back.md) | `turn-back` | yes | yes (sunset, cancel, pref) |
| Water Boil Timer | [water_boil_timer.md](./water_boil_timer.md) | `water-boil-timer` | yes | yes (start, cancel) |
| Weather | [weather.md](./weather.md) | `weather` | yes | partly (screen, list) |
| Whistle | [whistle.md](./whistle.md) | `whistle` | yes | partly (SOS toggle) |
| White Noise | [white_noise.md](./white_noise.md) | `white-noise` | yes | partly (service starts) |

Tools without a user guide, and so without a file yet: Experimentation (`experimentation`, 36), Local Messaging (`local-messaging`, 39), Local Talk (`local-talk`, 40), and User Guide (`user-guide`, 35; reached from tools.md). Some are hidden unless experimental tools are enabled in Settings.
