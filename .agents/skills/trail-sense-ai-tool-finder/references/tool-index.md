# Trail Sense AI Assistant Tool Index

Generated from the registered tool list in `Tools.kt`, individual `*ToolRegistration.kt` files, and the in-app user guides in `app/src/main/res/raw/guide_tool_*.md`.

Use this document when the AI assistant needs to answer:

- Which Trail Sense tool matches a user's need.
- Where the user can find the feature in the app.
- How the user should use it.
- What the displayed values, estimates, or results mean.

## Response Pattern

For normal user questions, answer in this structure:

1. Recommended tool: name the single best tool.
2. Where: `Tools` tab/list, search, pinned tool, quick action, widget, or exact settings path.
3. How: give 2-5 steps.
4. Values: explain the key readings or results.
5. Caveat: mention sensor availability, calibration, permissions, or safety limitations.

Do not provide raw implementation paths to app users unless they are developers.

## General Navigation

- Most tools are opened from the `Tools` tab/list. Users can search, pin tools, sort tools, or add quick actions and in-app widgets.
- Tool-specific settings live under `Settings`, often as `Settings > <Tool name>`.
- Built-in documentation is available from the `User Guide` tool or a guide button from a tool page.
- If a user asks for a feature that is not visible, check whether the tool depends on hardware, a plugin, debug builds, or experimental settings.

## Registered Tools

| ID | Tool | Category | Best for | Where / how to use | Values and result meanings | Availability / caveats |
|---:|---|---|---|---|---|---|
| 1 | Flashlight | Signaling | Light, strobe, SOS, screen light, signaling for help | Open `Flashlight`; use the main light, strobe, SOS, or screen mode. Can be added as a quick action or widget. | Modes represent the light pattern: steady light for visibility, strobe for attention, SOS for distress signaling, screen light when camera flash is unavailable or too harsh. | Depends on camera flash for torch modes; carry a real flashlight as backup. |
| 2 | Whistle | Signaling | Audible signaling, SOS/help tones | Open `Whistle`; choose whistle, SOS, help, or another signal. Can be a quick action. | The selected signal determines the sound pattern. SOS is the international distress rhythm. | Turn volume up or connect a speaker; carry a real whistle as backup. |
| 3 | Ruler | Distance | Measure small objects or distances on a paper map | Open `Ruler`; place the object along the on-screen ruler. Change units/direction in the tool. Use calibration if screen scale is off. | Displayed length is the measured physical distance in the chosen unit. Map measuring converts paper distance using the chosen map scale. | Accuracy depends on screen calibration and correct map scale. |
| 4 | Pedometer | Distance | Track steps, distance, speed, distance alerts | Open `Pedometer` and start tracking. Calibrate step length for better distance. Can be a quick action or widget. | Steps are counted by the step sensor; distance is steps multiplied by calibrated stride; speed is derived from step/distance over time. | Requires step sensor for full function; calibration strongly affects distance. |
| 5 | Cliff Height | Distance | Estimate height of a cliff or vertical drop | Open `Cliff Height`; follow the tool's estimation flow. | The result is an estimated height, based on measured inputs such as angle/distance or timing depending on the chosen method. | Estimate only; do not use for dangerous climbing or rescue decisions without proper gear. |
| 6 | Navigation | Location | Compass, bearing navigation, beacon navigation, current location, speed/elevation | Open `Navigation`. Tap the navigation button to choose a beacon, tap the compass to set a bearing, or inspect location/elevation/speed panels. | Bearing is direction in degrees; distance is distance to target; ETA is estimated travel time; elevation change compares current elevation to target; accuracy/satellites describe GPS quality. | Compass needs calibration; GPS/elevation can be inaccurate. Trust real terrain and carry a physical compass/map. |
| 7 | Beacons | Location | Save places, navigate back, share/import waypoints | Open `Beacons`; tap `+` to create, import, or create from QR/GPX/OSM. Tap a beacon to view or navigate. Can be a quick action or widget. | Coordinates identify the saved location; distance/bearing show where it is relative to you; elevation and notes add context. | GPS accuracy affects saved/current position. Grouping and visibility control organization and map display. |
| 8 | Offline Maps | Location | Import image/PDF/Mapsforge maps for offline use | Open `Offline Maps`; tap `+`; import file, camera photo, blank map, or group. Calibrate image/PDF maps with two known points. | Calibration rotation shows how the photo map is aligned; scale enables distance/elevation/location overlays; visibility controls whether a map appears in map layers. | Maps are not bundled. Non-geospatial images need calibration. Bad calibration causes misplaced beacons/paths. |
| 9 | Paths | Location | Record a hike, Backtrack, import/export GPX/KML/GeoJSON, follow/inspect trails | Open `Paths`; use Backtrack to record automatically or `+` to create/import. Tap a path for overview. Can be a quick action or widget. | Distance is path length; waypoints are recorded points; elevation profile shows ascent/descent; average speed and ETA depend on recorded movement. | GPS quality and recording frequency affect path accuracy and battery use. |
| 10 | Triangulate Location | Location | Find your location or a distant object's coordinates using two known bearings | Open `Triangulate Location`; enter two known locations and bearings, then view the intersection on the map. | The intersection is the estimated position; bearings are sight lines from known points. | Small bearing/location errors can create large position errors. Best with distant, well-spaced reference points. |
| 11 | Clinometer | Angles | Measure slope, angle, avalanche risk, estimate tree height/distance | Open `Clinometer`; hold the phone aligned with the slope or sightline. Use the relevant calculator mode. | Angle is inclination in degrees; slope may be shown as grade/percent; height/distance estimates are derived from angle plus known distance/height. | Requires calibrated orientation sensors. Avalanche guidance is only a rough signal, not a safety guarantee. |
| 12 | Bubble Level | Angles | Check if a surface is flat or level | Open `Bubble Level`; place the phone on the surface. | Bubble position and angle indicate tilt away from level; centered means approximately flat. | Depends on accelerometer calibration and phone case/edge shape. |
| 13 | Clock | Time | GPS time, digital clock, clock sync | Open `Clock`; view GPS time or digital time. | GPS time comes from satellites; sync information indicates offset/quality when available. | GPS time requires satellite data; indoor availability may be poor. |
| 14 | Astronomy | Time | Sunrise, sunset, moonrise, moonset, meteor showers, eclipses, alerts | Open `Astronomy`; inspect sun/moon cards, charts, event lists, or 3D view. Can provide alerts and widgets. | Rise/set times show when the object crosses the horizon; azimuth is compass direction; altitude is angle above horizon; phase/illumination describe moon appearance. | Calculations depend on location/time. Horizon obstructions can change visible sunrise/sunset. |
| 15 | Water Boil Timer | Time | Decide how long to boil water at current elevation | Open `Water Boil Timer`; use current or entered elevation and start the timer. | Boil time is the recommended duration after water reaches a rolling boil, adjusted because boiling temperature changes with elevation. | Guidance only; follow local health guidance and use proper treatment when possible. |
| 16 | Tides | Time | Track tide tables, nearby tide, tide chart, map tide layer | Open `Tides`; create a tide table from official high/low data or estimate without a table. Use chart or widgets. | High/low tide times are extrema; current tide height/phase is estimated between known points; chart shows rise/fall over time. | Tide estimates can be dangerous if wrong. Use official tide tables for boating, coastal travel, and safety. |
| 17 | Battery | Power | Battery consumption, time until charged/dead, power saving | Open `Battery`; inspect status, statistics, history, services, and system settings. Can be a quick action. | Time until charged/dead is an estimate from recent drain/charge; statistics show consumption and battery state over time. | Estimates change with usage, temperature, and Android battery reporting. |
| 18 | Solar Panel Aligner | Power | Aim a solar panel while camping | Open `Solar Panel Aligner`; follow the sun alignment guidance for orientation/tilt. | Alignment guidance indicates the direction/angle that should improve solar exposure. | Requires device orientation sensors and clear sun exposure; panel hardware and shade matter. |
| 19 | Light Meter | Power | Measure flashlight beam distance or light intensity | Open `Light Meter`; follow beam distance testing instructions. | Beam distance is the estimated usable throw distance; light readings are based on device light sensor/camera behavior. | Requires supported sensors; measurements are approximate and affected by environment. |
| 20 | Weather | Weather | Offline weather prediction, pressure trend, storm alert, weather monitor | Open `Weather`; enable monitor with the start icon for pressure history. Configure alerts in `Settings > Weather`. Can be quick action/widgets. | Falling pressure can mean incoming storms; rising pressure suggests clearing; pressure tendency is change over time; prediction combines pressure, climate normals, and logged clouds. | Requires barometer for core features. Forecast is a best guess, not an Internet forecast. Trust visible weather and seek shelter if needed. |
| 21 | Climate | Weather | Historical climate normals for a date/location | Open `Climate`; change date or location as needed. | Temperature/precipitation are historical averages/normals; classification/ecology summarize typical climate and biome patterns. | Normals are not the actual current weather and can differ during storms or unusual years. |
| 22 | Temperature Estimation | Weather | Estimate temperature at a different elevation | Open `Temperature Estimation`; enter current/target elevation and temperature if prompted. | Estimated temperature changes with elevation using a lapse-rate model. | Rough estimate only; weather fronts, shade, wind, and terrain can dominate. |
| 23 | Clouds | Weather | Identify cloud type and improve weather prediction | Open `Clouds`; log a cloud manually or use automatic identification. Can be a quick action. | Cloud type indicates likely weather pattern; logged clouds can inform the Weather tool's prediction/front detection. | Automatic identification may be wrong; use visible sky judgment. |
| 24 | Lightning Strike Distance | Weather | Estimate how far away lightning is | Open `Lightning Strike Distance`; start timing at flash and stop at thunder. | Distance is computed from the delay between light and sound. Increasing/decreasing distances indicate storm movement. | If thunder is audible, lightning risk exists. Seek shelter; do not wait for a "safe" app value. |
| 25 | Augmented Reality | Other | View beacons, sun/moon paths, stars/planets, paths, compass/grid over camera | Open `Augmented Reality`; point the camera around you and enable layers. | Overlays show estimated real-world direction/position; reticle and grid help align orientation. | Requires camera, compass, location, and calibration. AR can drift; verify with map/compass. |
| 26 | Convert | Other | Convert coordinates, distance, temperature, volume, weight, time | Open `Convert`; choose conversion type and enter a value. Can be quick action. | Output is the equivalent value in the target unit or coordinate format. | Coordinate conversions require correct datum/format assumptions. |
| 27 | Packing Lists | Other | Plan trip gear, quantities, packed status, pack weight | Open `Packing Lists`; create a list, add items, mark packed, sort/export/import. | Pack weight sums item weights and quantities; packed state tracks what is ready. | Weight is only as accurate as entered item data. |
| 28 | Metal Detector | Other | Detect magnetic metals like iron | Open `Metal Detector`; calibrate, move phone near objects, adjust threshold/sensitivity. | Readings represent magnetic field strength; spikes/direction changes suggest ferromagnetic metal nearby. Threshold controls alert sensitivity. | Cannot reliably detect coins/jewelry. Affected by phone magnets, cases, and environment. |
| 29 | White Noise | Other | Sleep sound, ambient sound, sleep timer | Open `White Noise`; choose sound and timer. Can be quick action. | Sound selection chooses audio texture; sleep timer stops playback after the set duration. | Keep volume safe, especially with headphones. |
| 30 | Notes | Other | Offline notes, QR sharing | Open `Notes`; create/edit/delete notes or share via QR code. Can be quick action. | Notes are stored text; QR shares note content for another device to scan. | Avoid storing sensitive data unless the device is protected. |
| 31 | QR Code Scanner | Other | Scan QR codes, open location/URL/text, import shared data | Open `QR Code Scanner`; point camera at code. | Result type determines action: URL opens link, location can open/create location data, text is displayed/copied. | Requires camera. Only open trusted URLs/codes. |
| 32 | Sensors | Other | Inspect available sensors and raw sensor values | Open `Sensors`; view sensor list and details. Can be quick action/widgets. | Values are raw or processed device sensor readings such as pressure, light, orientation, acceleration, GPS, etc. | Useful for troubleshooting; units and availability depend on hardware. |
| 33 | Diagnostics | Other | Detect common configuration/sensor/service problems | Open `Diagnostics`; run or inspect the diagnostics list. | Each diagnostic reports whether a dependency such as GPS, compass, barometer, permissions, notifications, or background service is healthy. | Passing diagnostics does not guarantee perfect readings. |
| 34 | Settings | Other | Configure units, sensors, privacy, theme, tools, backup, calibration | Open `Settings`; choose category. Can be quick action. | Settings change how tools interpret and display values: units, compass north reference, sensor source, smoothing, offsets, backups, privacy, and tool visibility. | Incorrect calibration/settings can affect many tools. |
| 35 | User Guide | Books | Read built-in help for tools | Open `User Guide`; search or select a guide. Can be quick action. | Guide content explains usage, settings, accuracy, widgets, and related tools. | It is static documentation and may not include experimental/plugin tools. |
| 36 | Experimentation | Other | Internal experimental/debug features | Normally not visible; registered only in debug builds. | No stable user-facing result contract. | Debug-only and experimental; do not recommend to regular users. |
| 37 | Mirror Camera | Other | Use phone camera as a mirror | Open `Mirror Camera`; use front camera view. | The displayed image is the camera preview. | Requires camera; not a medical tool. |
| 38 | Turn Back | Time | Alert when to turn back to return by a chosen time or before dark | Open `Turn Back`; choose return time or return-before-dark behavior. | Alert time is based on the target return time and configured logic; "before dark" uses local sunset context. | Estimate depends on assumptions and user pace; leave margin. |
| 39 | Local Messaging | Communication | Open companion communications plugin messaging tool | Visible only if `com.kylecorry.trail_sense_comms` is installed; opening it launches the companion app with the messaging tool. | Values/results are handled by the companion communications app, not core Trail Sense. | Experimental/plugin-only; no built-in user guide in this repo. |
| 40 | Local Talk | Communication | Open companion communications plugin voice/talk tool | Visible only if `com.kylecorry.trail_sense_comms` is installed; opening it launches the companion app with the talk tool. | Values/results are handled by the companion communications app. | Experimental/plugin-only; no built-in user guide in this repo. |
| 41 | Survival Guide | Books | Wilderness survival reference | Open `Survival Guide`; browse or search topics. Can be quick action. | Results are reference articles/steps, not live sensor readings. | Educational only; follow real safety guidance and local emergency procedures. |
| 42 | Field Guide | Books | Identify plants, animals, objects; record sightings | Open `Field Guide`; browse built-in pages, create/edit pages, record sightings, control map layer visibility. | Sightings are saved observations; map layer visibility controls whether they appear on maps. | Identification is not guaranteed; be careful with plants/animals/mushrooms. |
| 43 | Signal Finder | Signaling | Find places with cell signal and view nearby cell towers/signals | Open `Signal Finder`; inspect signals/towers and emergency-call info. | Signal readings indicate cellular reception strength/availability; tower info helps choose a better location. | Cell data availability varies by device/network; emergency availability is not guaranteed. |
| 44 | Ballistics | Other | Hunting firearm calculators: scope, trajectory, energy | Open `Ballistics`; enter firearm/projectile/scope parameters. | Drop/adjustment/energy values are calculator outputs from entered parameters. | Safety-critical. Follow firearm laws and safe handling; verify with real ballistic data. |
| 45 | Permits | Other | Placeholder/experimental permit list | Debug-only experimental tool; currently registered with an empty permit list fragment. | No stable user-facing values. | Do not recommend to regular users yet. |
| 46 | Declination | Location | Determine magnetic declination for a physical compass | Open `Declination`; use current or chosen location. | Declination is the angle between true north and magnetic north; set this on a physical compass or configure Trail Sense compass settings. | Declination varies by location and date. |
| 47 | Map | Location | View beacons, paths, offline maps, layers, elevation, measurements | Open `Map`; pan/zoom, use layers, long-press to create/navigate/measure, change time if needed. Has widget. | Scale shows map distance; distance/elevation measurements come from selected map/data sources; layers show beacons, paths, tides, DEM overlays, etc. | Not a replacement for dedicated maps or physical maps. Needs loaded offline maps for base map content. |
| 48 | Magnifier | Other | Magnify close-up objects/text with camera | Open `Magnifier`; adjust zoom, focus, torch, or freeze image. | Zoom is camera magnification; freeze captures the current frame for inspection. | Requires camera. Image quality depends on focus and light. |
| 49 | AI Assistant | Other | Chat-based assistant for app help/contextual support | Open `AI Assistant`; configure via AI settings where available. | Responses should be grounded in tool guides and live app context when available. | Registered as a normal tool, but it has no user guide yet in the raw guides. |

## Scenario Routing

Use this section to pick a tool quickly from a user's natural-language need.

| User need | Recommend | Related tools |
|---|---|---|
| "I need light" / "signal SOS visually" | Flashlight | Whistle, Signal Finder |
| "Make a loud emergency signal" | Whistle | Flashlight |
| "Measure an object" | Ruler | Convert |
| "Measure slope/angle/tree height/avalanche angle" | Clinometer | Bubble Level |
| "Check if my stove/tent platform is flat" | Bubble Level | Clinometer |
| "Find my way to a saved place" | Navigation | Beacons, Map |
| "Save this location" | Beacons | Navigation, Map |
| "Record my hike or find my way back" | Paths | Navigation, Map, Pedometer |
| "Use maps without Internet" | Offline Maps | Map, Map Layers |
| "See all saved locations/paths on a map" | Map | Beacons, Paths, Offline Maps |
| "Find a location from bearings" | Triangulate Location | Navigation, Compass settings |
| "Know magnetic declination" | Declination | Navigation, Settings > Sensors > Compass |
| "Know sunrise/sunset/moon/event times" | Astronomy | Clock, Navigation |
| "Set an alert to turn around" | Turn Back | Astronomy, Paths |
| "Know tide timing" | Tides | Map |
| "Predict weather without Internet" | Weather | Clouds, Climate |
| "Identify clouds" | Clouds | Weather |
| "How far away was lightning?" | Lightning Strike Distance | Weather |
| "Estimate temperature at another elevation" | Temperature Estimation | Weather, Climate |
| "Historical average weather" | Climate | Weather |
| "Make water safe by boiling" | Water Boil Timer | Survival Guide |
| "Aim solar panel" | Solar Panel Aligner | Astronomy |
| "Check battery drain" | Battery | Settings |
| "Find cell signal" | Signal Finder | Beacons, Map |
| "Detect iron/keys/knife" | Metal Detector | Sensors |
| "Scan or share QR" | QR Code Scanner | Notes, Beacons |
| "Write an offline note" | Notes | QR Code Scanner |
| "Pack for a trip" | Packing Lists | Notes |
| "Identify plant/animal/sighting" | Field Guide | Survival Guide |
| "Read survival reference" | Survival Guide | User Guide |
| "Troubleshoot the app" | Diagnostics | Sensors, Settings |
| "Change units/calibrate sensors/privacy/theme" | Settings | Diagnostics |
| "Need help using a Trail Sense tool" | User Guide | AI Assistant |

## Value Interpretation Notes

- Bearing/azimuth: degrees clockwise from north. North reference may be true north or magnetic north depending on settings.
- Distance: straight-line distance unless the tool says it is path length.
- ETA: estimated time based on current or configured speed; it changes with pace and terrain.
- Elevation: may come from GPS, barometer, DEM, or manual calibration depending on settings.
- Accuracy: a radius/quality indicator for GPS or sensor confidence; lower GPS accuracy distance is better.
- Pressure tendency: pressure change over time; falling often indicates worsening weather, rising often indicates clearing.
- Climate normals: historical averages, not live weather.
- Tide estimates: interpolation or model output from tide tables; use official data for safety.
- Magnetic field: high or changing readings can suggest ferromagnetic metal, but many phone/environment factors can interfere.
- Lux/beam distance/light: approximate device-sensor readings, not lab-grade measurements.
- Battery time remaining: estimate from recent usage and Android-reported battery state.

## Missing Or Special Documentation

The following registered tools have limited or no in-app guide coverage in the current source tree:

- `AI Assistant`: registered tool with settings, no guide file.
- `Experimentation`: debug-only experimental tool, no guide file.
- `Local Messaging`: plugin-only communications tool, no guide file.
- `Local Talk`: plugin-only communications tool, no guide file.
- `Permits`: debug-only placeholder/experimental tool, no guide file.
- `User Guide`: registered tool, covered indirectly by `guide_tool_tools.md`.

When asked about these, mention their limited availability and avoid promising stable behavior.
