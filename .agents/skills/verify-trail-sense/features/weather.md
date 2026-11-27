# Weather

Use the Weather tool to roughly predict the weather without an Internet connection. The user guide has the procedures. This file adds how to drive and prove them.

## Sub-features

Every section of [`guide_tool_weather.txt`](../../../../guides/en-US/guide_tool_weather.txt) is a sub-feature. Use its headings as the feature ids when reporting what was verified.

## How to get to it (user POV)

- Tools tab → search `Weather` → tap the tile.
- Bottom navigation tab `Weather` (default tabs).
- Quick action (see the guide's Quick action section).
- Home screen or in-app widgets (see the guide's Widgets section).
- Precondition shortcut only: `$T open weather`.

## Driving it with tsctl

Preconditions:

- `$T doctor` ends with `doctor: OK` after `$T app seed`.
- Barometer readings: `$T env sensor set pressure 1013.25`.

- **Prediction.** Tap the Weather tab. Run `$T ui tap desc=Weather` (or `$T open weather`). A `Historic temperatures` dialog shows on first open (`id=button1`). The title shows the forecast (`No change`) and `id=weather_list` lists Alerts, Pressure (`30.09 in` for 1013.25 hPa: units default to inHg), Tendency, Temperature and `Weather monitor` (`Off - 30m`); `id=play_btn` starts the monitor.
- **History.** Start the weather monitor and feed falling pressure over time. Run `$T env sensor set pressure 1005` ... `995` between recordings. Pressure trend and storm alert after a steep drop.
- **Side effect.** Stored readings. Run `$T db query "select * from pressures order by time desc limit 5"`. Rows per recording; a new reading is skipped when the last is newer than one third of the update frequency (default 30 min), so shorten the frequency (tap the time under `Weather monitor`) or delete rows between samples.
- **Proof.** Snap the weather screen; save the db output.

## Gotchas

- The monitor records on an interval; shorten it in the weather settings or the run takes hours.
- Clouds logged in the Clouds tool feed into the prediction.
- To get a realistic chart without waiting hours, write an exported weather CSV into `pressures` with `$T db exec` (columns `pressure, altitude, altitude_accuracy, temperature, humidity, time, latitude, longitude`), with the times shifted so the last row is a few minutes old.
- A "System UI isn't responding" dialog can cover the app right after boot. Dismiss it with `$T ui tap 'text=/^Wait$/'`.

## Forecast accuracy benchmark

`WeatherForecastBenchmarkTest` replays hourly airport observations through `WeatherForecaster` and `PressureForecaster`, then scores the results against what each station observed afterward. The data is gzipped CSV in `app/src/test/resources/weather_benchmark/`:

- US airports for a year (Iowa Environmental Mesonet ASOS), covering tropical, desert, mountain, plains, marine, lake-effect, continental, arctic and southern hemisphere climates, plus mountain and trailhead towns where people hike (Jackson Hole, Aspen, Flagstaff, Bozeman, Juneau and others)
- Hurricanes Ida, Ian and Milton (about two weeks each)
- Airports on other continents for January to August 2025 (NOAA global hourly), including the Alps, the Andes, Patagonia, the Himalaya, Scotland, Norway, the Rockies and Tasmania. Weather, sky and gusts come from the raw METAR in each record. Stations without hourly sea level pressure use the altimeter setting (QNH), which is usually whole hPa. High Andes stations read QNH well above 1013 hPa; that bias is kept, since a phone at altitude is off in the same way.

Refresh or change it with `python3 scripts/download_weather_benchmark.py`, or pass station ids to download only those and merge them into `stations.csv`. The stations and dates are at the top of that script. Drop a station whose reports can't be trusted: no sky reports, almost never BKN/OVC or almost never clear (sky not reported), or far fewer precipitation hours than its climate normal (weather codes missing). The script lists the stations already dropped and why.

- Run it with `$T test unit '*WeatherForecastBenchmarkTest*'` (a few minutes; stations run in parallel and each hour is forecast once), or one part with `.benchmark` or `.shortHistory`. The class is tagged `manual`, so a full `test unit` run skips it.
- `benchmark` writes `report.md` and `results.csv` to `app/build/reports/weather-benchmark/`. These stations are the network's training data, so its scores are optimistic; trust the training script's held out numbers.
- Each report starts with a data coverage table (hours of each weather type per station), then each metric.
- What counts as a correct call. Each condition is scored as the app labels it: `_soon` is correct when the condition happens in the next 3 h, and `_later` is correct when it does not happen in the next 3 h but does in 3 to 12 h.
  - Precipitation: measurable precipitation (0.25 mm or more, or any precipitation code) in the window
  - Storm: thunder, gusts of 35 kt or more, or 6 mm/h or more, in the window
  - Wind: gusts of 25 kt or more, or sustained wind of 20 kt or more, in the window
  - Clear / Overcast: most reports in the window show clear-to-few (CLR/FEW) or broken-to-overcast (BKN/OVC) clouds. US ASOS only reports clouds below 12,000 ft, while the other airports report all layers, so their skies score lower.
  - Thunderstorm: thunder within 12 h, with no soon or later
  - `_12h_to_24h`: the daily forecast's precipitation, clear and overcast, 12 to 24 hours out
  - Rain vs Snow: when the app names a type and precipitation follows, whether the first report was frozen
- Metrics:
  - Each condition reports `pod` (share of events warned about), `far` (share of calls that were wrong), `csi`, and `hss` (Heidke skill: 1 is perfect, 0 is chance, below 0 is worse than chance). Compare conditions by `hss`.
  - Under `ALL`, `station_mean_*_hss` is the mean of each station's score. Judge changes by it: the pooled `hss` also rewards telling rainy places from dry ones, which is not skill at a place.
- Temperatures and precipitation come from the app's climate model, as in the app. `climate_normals.csv` holds each station's monthly low, high and precipitation from `HistoricMonthlyTemperatureRangeRepo` and `HistoricMonthlyPrecipitationRepo`. Those models are webp images that only Android can decode, so after adding a station, regenerate the file on the emulator: write a throwaway `androidTest` that logs both for each station, run it with `$T test android`, and collect the lines with `$T logs --grep`.
- The pressure tendency, front and logged cloud conditions still come from Sol's `Meteorology.forecast`. To test unpublished Sol changes, add `includeBuild("../sol") { dependencySubstitution { substitute(module("com.github.kylecorry31:sol")).using(project(":")) } }` to `settings.gradle.kts`, and remove it before committing.
- `$T test unit '*WeatherForecastBenchmarkTest.shortHistory'` reruns the benchmark with only 3, 6, 12 and 48 hours of readings, as for a monitor that was just turned on, and writes `short-history.md`. Every condition should stay above 0 (chance).
- To compare a change, copy `results.csv` somewhere outside `app/build` before rerunning, then compare the two files.
- Gradle skips the test when nothing has changed since the last run, and the previous report stays in place.

## Weather network

The conditions predicted from pressure come from a small neural network in `forecasting/model/`, run with Sol's `NeuralNetwork`. Logged clouds still come from Sol's cloud rules, and without a climate (the lookup failed) only logged clouds give conditions. It has one sigmoid output per condition, so each condition gets its own probability, shown next to the condition in the forecast dialog. Its inputs are the pressure history, the day's climate temperatures, the month's climate precipitation and the solar time of day.

Each condition has two outputs, for the next 3 hours (`_next_3h`, including weather already happening) and for starting in 3 to 12 hours (`_3h_to_12h`: not in the next 3 hours, but in 3 to 12 hours); the app shows the condition as "within 3 hours" when the first passes its threshold, otherwise "in 3–12 hours" when the second does. The arrival under the prediction is the main condition's soon or later, unless logged clouds give a time. Thunderstorm has one output for the next 12 hours, because the 3 hour window had no skill (held out HSS 0.02). The daily forecast uses the `_12h_to_24h` outputs. Finer timing was tested and rejected: on held out stations, 1, 3, 12 and 24 hour onset windows beat always guessing the most common one only for wind. A separate "happening now" output was tested and rejected: pressure can't tell weather already happening from weather about to start, so splitting them made the next 3 hours worse. Reading an arrival time off now, 1, 3, 6, 12 and 24 hour outputs was also rejected: its error was about 3 hours, the same as always guessing the typical arrival. A later forecast is stored as 4 hours out (for the rain or snow temperatures), because later weather usually arrived 3 to 5 hours out. The 3, 12 and 24 hour pressure changes come from a line fit to the readings in each window, which scored slightly better on held out stations than the change between two readings; the app and the training script must compute them the same way (`WeatherModel.getPressureChange` and `change` in the script).

Each output is called when its probability passes a threshold chosen on held out stations. The forecast and storm sensitivity settings move that threshold: low doubles the odds needed and high halves them. Storm sensitivity applies to Storm and forecast sensitivity to every other condition.

To retrain after changing the benchmark data:

1. `$T test unit '*ClimateFeatureExport*'` writes the app's daily climate temperatures for each station to `app/build/reports/weather-benchmark/climate_features.csv`.
2. `python3 scripts/train_weather_model.py` (needs numpy, takes about 5 minutes) trains the network, prints how it scores on stations it was not trained on, and rewrites `WeatherNetworkCoefficients.kt` and `weather_network_golden.csv`.
3. `$T test unit '*WeatherNetworkTest*'` checks that the app's network gives the same probabilities as the script.
4. Copy the old `results.csv` aside, run `.benchmark`, and compare.

Judge a new input by its held out numbers (station mean HSS and median Brier skill), not by the training stations. Inputs that identify a place, such as latitude or climate zone, scored better in training and worse on unseen stations. Fog, troughs, sun altitude and a latitude scaling of pressure changes added no held out skill.
