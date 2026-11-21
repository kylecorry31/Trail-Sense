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
- Barometer readings: `$T sensor set pressure 1013.25`.

- **Prediction.** Tap the Weather tab. Run `$T ui tap desc=Weather` (or `$T open weather`). A `Historic temperatures` dialog shows on first open (`id=button1`). The title shows the forecast (`No change`) and `id=weather_list` lists Alerts, Pressure (`30.09 in` for 1013.25 hPa: units default to inHg), Tendency, Temperature and `Weather monitor` (`Off - 30m`); `id=play_btn` starts the monitor.
- **History.** Start the weather monitor and feed falling pressure over time. Run `$T sensor set pressure 1005` ... `995` between recordings. Pressure trend and storm alert after a steep drop.
- **Side effect.** Stored readings. Run `$T db query "select * from pressures order by time desc limit 5"`. Rows per recording; a new reading is skipped when the last is newer than one third of the update frequency (default 30 min), so shorten the frequency (tap the time under `Weather monitor`) or delete rows between samples.
- **Proof.** Snap the weather screen; save the db output.

## Gotchas

- The monitor records on an interval; shorten it in the weather settings or the run takes hours.
- Clouds logged in the Clouds tool feed into the prediction.
