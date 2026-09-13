---
name: trail-sense-backtrack-session-summary
description: Summarize a Trail Sense backtrack recording session on a connected Android device as a timeline table and ranked findings, using the adb scripts in scripts/debugging.
---

# Trail Sense Backtrack Session Summary

Explain what happened during a backtrack session on the connected device: when points were recorded, why gaps happened, and how much battery was used.

## Session

Summarize the session the user names. If they don't name one, summarize the last backtrack session without asking. Treat anything else the user mentions, such as battery readings, airplane mode, or changed settings, as extra context to check against the logs.

## Scripts

All scripts are in `scripts/debugging/`, read the connected device with adb, and take `--start` and `--end` as `"YYYY-MM-DD HH:MM"` or `"HH:MM"` in device time. Pass the package as the first argument when it isn't the default.

| Script | Shows |
|---|---|
| `list_backtrack_points.py` | Backtrack settings, recorded point count, time between points, gaps with missed point counts, system battery level and charge, app battery log. Without a time window, it uses the last backtrack session and prints that window for the other scripts. Requires a debuggable build. |
| `list_doze_events.py` | DOZE (off/light/full), SCREEN, and PLUG changes from battery stats, plus detailed DEEP/LIGHT Doze steps from logcat |
| `list_gps_events.py` | Each location listener registration with start, first location, end, duration, and request; GPS throttle or disable events; GPS hardware on-time |
| `list_wakelocks.py` | Each wakelock the app held, with start, end, and duration |
| `list_app_logs.py` | The app's own log file (`cache/log.txt`): GPS accuracy filter rejects, GPS timeouts, and bad reading or Kalman filter resets, with a count of each message type. Requires a debuggable build. |

Logcat is a rolling buffer, so detailed Doze steps, GPS registrations on older Android versions, and Samsung wakelocks disappear within hours. Run the scripts before anything else.

## Workflow

1. Identify the device and build. This step is complete when the model, Android version, SDK, app version, and install time are known.
   ```
   adb devices -l
   adb shell getprop ro.product.model
   adb shell getprop ro.build.version.release
   adb shell getprop ro.build.version.sdk
   adb shell dumpsys package com.kylecorry.trail_sense | grep -E "versionName|firstInstallTime|lastUpdateTime"
   ```
   If more than one device is connected, ask which one and set `ANDROID_SERIAL`.

2. Run `list_backtrack_points.py` and save its output to a scratch file. This step is complete when the session window is known.
   - No session named: run it with no time window. It summarizes the last backtrack session and prints a `Window for the other debugging scripts:` line with `--start` and `--end` values.
   - Session named: pass that `--start` and `--end`.
   ```
   python3 scripts/debugging/list_backtrack_points.py > <scratch>/points.txt
   ```

3. Run the other four scripts with the same window and save each output. This step is complete when every script has finished and its output is saved.
   ```
   python3 scripts/debugging/list_doze_events.py --start "<start>" --end "<end>" > <scratch>/doze.txt
   python3 scripts/debugging/list_gps_events.py --start "<start>" --end "<end>" > <scratch>/gps.txt
   python3 scripts/debugging/list_wakelocks.py --start "<start>" --end "<end>" > <scratch>/wakelocks.txt
   python3 scripts/debugging/list_app_logs.py --start "<start>" --end "<end>" > <scratch>/app_logs.txt
   ```

4. Read the `Note:` lines in every output and write down which periods each source doesn't cover. This step is complete when you know which parts of the session can't be explained from the logs.

5. From `points.txt`, record the point count, the expected count, the median time between points, and every gap. This step is complete when every gap has a from time, to time, and missed point count.

6. Explain each gap by lining up the other outputs, including `app_logs.txt`, around its from and to times. Use the reference below. This step is complete when every gap has a cause backed by specific timestamps, or is marked as unexplained.

7. Check the recording interval. If the median time between points is well above the setting, break one cycle down using `gps.txt` and `wakelocks.txt`: run length, GPS restart delay, time to first fix, and the wait for the next location. This step is complete when the extra time is accounted for.

8. Summarize battery use from `points.txt` and GPS on-time from `gps.txt`. Compare them with any values the user mentioned. This step is complete when start, end, change, and average current are known, and any disagreement with the user's numbers is noted.

9. Write the summary using the output format below. This step is complete when every claim cites a timestamp or log value, and inferences are labeled as likely.

## Interpreting the Outputs

### How backtrack schedules runs

These mechanics were current in September 2026. If a finding depends on one, check `BacktrackService`, Andromeda's `IntervalService`, and Luna's `FlowableTimer` for changes first.

- With keep device awake off and the device GPS as the source, the next run is started by a GPS listener at the backtrack interval. It shows as an `every 1m` registration for the default interval.
- After each run, the listener registers again and `FlowableTimer` discards its first location. The time between points is therefore about: run length + GPS restart delay + time to first fix + interval.
- Each run requests a single fix (`every 20ms`), holds the `BacktrackService` wakelock until it finishes, and waits at most `SensorService.GPS_READ_TIMEOUT` (30s) for a fix.
- With keep device awake on, a continuous wakelock and a coroutine timer drive runs instead, and there is no GPS wake listener.

### Signals

| Signal | Meaning |
|---|---|
| `DOZE full` or `DEEP LOCATING -> IDLE` | Deep Doze started. It needs the screen off, no charger, and a still phone for a while. |
| `DEEP IDLE -> IDLE_MAINTENANCE` then `IDLE_MAINTENANCE -> IDLE` | A maintenance window. Usually about 30s, with hours between them later in the night. |
| `DEEP ... -> ACTIVE (motion)`, `(unlocked)`, or `(screen)` | Deep Doze ended because the phone moved, was unlocked, or the screen turned on |
| `gps provider stationary/idle throttled` (seen on Android 14+) | Android turned GPS off while in Deep Doze and still |
| `gps provider disabled by power manager (Doze)` (seen on Android 10) | Android fully disabled GPS for Deep Doze |
| An `every 1m` registration lasting much longer than the interval | The wake listener was waiting for a location that never came, usually because GPS was off |
| An `every 20ms` registration lasting about 30s or more | The run hit the GPS timeout without a fix. Far beyond 30s means the CPU slept during the run. |
| FIRST LOCATION minus START | Time to first fix. It grows after GPS has been off for hours, so a 30s maintenance window can pass without a fix. |
| Long delay between a wake listener registering and the GPS actually starting | Likely the wakelock was released before the registration reached the GPS, so it waited for the next CPU wake. In Light Doze that can be minutes. |
| `BacktrackService` wakelock of 1–2ms | The wakelock doesn't cover the run, so a run can stall while the CPU sleeps |
| `BacktrackService` wakelock of a few seconds | The wakelock covers the run, as expected |
| `AccuracyFilterGPSModule Reject: <accuracy> > <limit>` | A fix was worse than the GPS accuracy setting, so the run kept waiting |
| `AccuracyFilterGPSModule Accept (breaker tripped)` or `(breaker open)` | The run gave up waiting for an accurate fix and took the best recent one |
| `TimeoutGPSModule Timed out after 30s` | No usable fix arrived before the GPS read timeout, and the previous reading was kept |
| `BadReadingFilterGPSModule Accepted: last reading is in the future` | The previous fix's time was ahead of the phone's clock, which usually means the phone clock and GPS time disagree |
| `KalmanGPSModule Kalman filter reset: fix time moved backward` | A fix was older than the previous one, so smoothing restarted |
| No app log entries during a period | Nothing unusual was logged. The app only logs these exceptions. |

### Caveats

- `LOCATIONS` means per-app deliveries when the GPS output reads the location event log (Android 14+ so far). When it reads logcat instead (Android 10 so far), it counts every GPS fix the device reported while the listener was registered, including fixes other requests caused.
- On older Android versions (Android 10 so far), battery stats times are rebuilt from anchors with second precision, so DOZE rows can be up to 1s off from DEEP rows.
- GPS hardware on-time covers all apps. Airplane mode and no other location apps make it close to Trail Sense's own use.
- The charge counter updates rarely and in coarse steps (about 27 mAh on some Samsung phones), so treat mAh values as approximate.
- A battery level reported long before the window started means the phone was off or battery stats has a gap. The script shows the report time when it's stale.
- Samsung wakelock rows get their start time from the logged hold duration.
- `cache/log.txt` keeps about the last 256 KB and drops the oldest quarter when full, so long or noisy sessions can lose their start.

## Output Format

```markdown
**<One-line result: points recorded vs. expected, and the main cause.>**

Device: <model>, Android <version> (SDK <sdk>), <package> <version> installed <time>
Session: <start> → <end> (<duration>), plus any conditions the user mentioned

## Timeline

| Time | What happened | Points |
|---|---|---|
| <time or range> | <event, with the Doze or GPS state that explains it> | <count> |

## Findings, most important first

1. **<Finding.>** <Evidence with timestamps or log values.>

## Battery

| | Start | End |
|---|---|---|
| System battery level | <level> | <level> |
| Charge counter | <mAh> | <mAh> |

- <Change and average current, GPS on-time, and any disagreement with user-reported values>

## Data gaps

- <Periods or values the logs can't explain, and why>
```

- Keep the timeline to the events that explain recording: start, normal recording ranges, each gap with its cause, Doze starts and ends, maintenance windows, and the end.
- Rank findings by how many points they cost.
- Say when a user-reported value doesn't match the logs instead of silently picking one.
