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

- **Prediction.** Tap the Weather tab. Run `$T ui tap desc=Weather`. Current pressure and a prediction.
- **History.** Start the weather monitor and feed falling pressure over time. Run `$T sensor set pressure 1005` ... `995` between recordings. Pressure trend and storm alert after a steep drop.
- **Side effect.** Stored readings. Run `$T db query "select * from pressures order by time desc limit 5"`. Rows per recording.
- **Proof.** Snap the weather screen; save the db output.

## Gotchas

- The monitor records on an interval; shorten it in the weather settings or the run takes hours.
- Clouds logged in the Clouds tool feed into the prediction.
- Handles marked "verify with `$T ui find`" were taken from the guide wording, not from a driven run. Confirm and update them here on first use.
