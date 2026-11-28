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

- **Prediction.** Tap the Weather tab. Run `$T ui tap id=action_weather` (or `$T open weather`). A `Historic temperatures` dialog shows on first open (`id=button1`). The title shows the forecast (`No change`) and `id=weather_list` lists Alerts, Pressure (`30.09 in` for 1013.25 hPa: units default to inHg), Tendency, Temperature and `Weather monitor` (`Off - 30m`); `id=play_btn` starts the monitor.
- **History.** Start the weather monitor and feed falling pressure over time. Run `$T env sensor set pressure 1005` ... `995` between recordings. Pressure trend and storm alert after a steep drop.
- **Side effect.** Stored readings. Run `$T db query "select * from pressures order by time desc limit 5"`. Rows per recording; a new reading is skipped when the last is newer than one third of the update frequency (default 30 min), so shorten the frequency (tap the time under `Weather monitor`) or delete rows between samples.
- **Forecast from a short history.** Write a few hours of falling readings at the user's interval into `pressures` (see Gotchas), relaunch, and tap the Weather tab. For 1.1 hPa falls every 50 minutes over 7 hours at 42.36, -71.06 the title shows `Rain` over `within 3 hours`; the forecast dialog lists each condition with `within 3 hours` or `in 3–12 hours` and its probability.
- **Forecast accuracy.** Run `$T test unit '*WeatherForecastBenchmarkTest*'`. It replays the airport stations in `app/src/test/resources/weather_benchmark/` through `WeatherForecaster` and writes `report.md`, `results.csv` and `short-history.md` to `app/build/reports/weather-benchmark/`. Compare the `station_mean_*_hss` rows under `ALL` against a copy of the previous `results.csv`; every short-history value should stay above 0.
- **Forecast from logged clouds alone.** With no pressure history (a fresh seed has a single reading), log Nimbostratus in the Clouds tool and tap the Weather tab: the title changes from `No change` to `Rain` over `within 3 hours`, and the dialog lists Overcast, Rain and Wind with probabilities. Proven on 2026-11-28. Clouds count for 4 hours after they are logged.
- **Tendency.** It is the network's 3 hour pressure difference with the daily tide removed, so it differs from the raw readings by up to about 0.5 hPa. The falling history above shows `-0.133 in / 3h` with a down arrow.
- **Rain or snow.** The network decides it from the climate, not a temperature rule. The same falling history at 71.29, -156.79 in November shows `Snow` over `within 3 hours`; at 42.36, -71.06 it shows `Rain`.
- **Network matches its training.** Run `$T test unit '*WeatherNetworkTest*'`. The app's network gives the same probabilities as `scripts/train_weather_model.py` wrote to `weather_network_golden.csv`.
- **Proof.** Snap the weather screen; save the db output and the benchmark report.

## Gotchas

- The monitor records on an interval; shorten it in the weather settings or the run takes hours.
- Clouds logged in the Clouds tool feed into the prediction.
- To get a realistic chart without waiting hours, write an exported weather CSV into `pressures` with `$T db exec` (columns `pressure, altitude, altitude_accuracy, temperature, humidity, time, latitude, longitude`), with the times shifted so the last row is a few minutes old.
- The benchmark stations are also the network's training data, so benchmark scores are optimistic. The held out scores printed by `scripts/train_weather_model.py` are the honest ones.
- `python3 scripts/train_weather_model.py --no-write` prints the held out scores (clean and with phone noise) without overwriting `WeatherNetworkCoefficients.kt` or the golden file, for trying training changes.
- A `test unit` filter that matches the benchmark package (such as `com.kylecorry.trail_sense.tools.weather*`) also runs the manual benchmark and `ClimateFeatureExport`, which rewrite the reports; save `results.csv` before such a run to compare.
- The benchmark is tagged `manual`, so a full `test unit` run skips it, and Gradle skips it when nothing changed and leaves the old report in place.
- Changing the network's inputs in `WeatherModel` without retraining (steps in the docstring of `scripts/train_weather_model.py`) fails `WeatherNetworkTest`.
- A "System UI isn't responding" dialog can cover the app right after boot. Dismiss it with `$T ui tap 'text=/^Wait$/'`.
