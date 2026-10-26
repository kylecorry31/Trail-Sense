# Proof of concept: navigate in background

## Goal

When the user starts navigation, a foreground service keeps running. Its notification shows the remaining distance and the ETA. For path navigation it also shows a progress bar. The GPS updates on an interval. The user can turn the feature off.

## Result

The idea is feasible. On an emulator (Android 16, mocked GPS):

- Beacon navigation showed `0.66 mi · 4h 23m (11:10 PM)`. The notification was removed when navigation was cancelled.
- Path navigation showed the path name, `1.35 mi · 40m (7:28 PM)` and a progress bar (2/100).

## How it works

- `NavigationForegroundService` extends `IntervalService`, like `BacktrackService`. Its foreground service type is `location`.
- The GPS is the interval timer. `getNonWorkerTimer` returns a `FlowableTimer` that wraps a `GPS` with `frequency = period`. The GPS only wakes the service.
- Each tick (`doWork`):
  1. `LocationSubsystem.updateLocation()` refreshes the location.
  2. `Navigator.getDestination()` returns the destination.
  3. The service builds the notification for that destination and calls `Notify.update`.
  4. If there is no destination, or the destination is a bearing, the service stops itself.
- Path: the service reads `destination.route.guidance`. `PathNavigator` updates it when the location changes, so the service does not call `route.navigate`. The ETA uses `HikingService.getHikingDuration`. The progress bar uses `guidance.progress`.
- Beacon: the distance is the straight line to the beacon. The ETA uses `NavigationService.eta`. There is no progress bar.
- `Navigator` starts the service on `BROADCAST_NAVIGATION_STARTED` if the user setting is on. It stops the service on `BROADCAST_NAVIGATION_STOPPED`.

## Changes

| File | Change |
|---|---|
| `tools/navigation/infrastructure/NavigationForegroundService.kt` | New. The service, the notification and `start`/`stop`. |
| `tools/navigation/infrastructure/Navigator.kt` | Subscribes to the started and stopped broadcasts in `init`. |
| `tools/navigation/infrastructure/NavigationPreferences.kt` | Adds `navigateInBackground` (default true) and `backgroundUpdateInterval` (default 60 s). |
| `tools/navigation/NavigationToolRegistration.kt` | Registers the `Navigation_Background` notification channel (low importance, no sound). |
| `AndroidManifest.xml` | Declares the service with `foregroundServiceType="location"`. |
| `res/xml/navigation_preferences.xml` | Adds the "Navigate in background" switch and the "Background update interval" list. The list depends on the switch. |
| `res/values/strings.xml`, `res/values/arrays.xml` | Preference keys, titles and the interval options (30 s, 1 min, 2 min, 5 min). |

## Not covered by the proof of concept

- The ETA uses a fixed speed (the minimum hiking speed, about 2 mph). The navigation screen uses the live speed.
- Navigation restored after the app process restarts does not start the service. The service starts only on the "navigation started" broadcast.
- A change to the interval applies the next time navigation starts.
- The notification has no Stop action.
- Bearing navigation is not supported.
- The GPS power mode is the global GPS setting. There is no separate navigation setting.
- The new strings are English only. There is no user guide update and no automated tests.
- Battery use was not measured. The interval options were not tested other than the 1 minute default.
- On the emulator, the service did not start without location permission. The system threw a `SecurityException`. Starting navigation from a non-visible app state was not tested.
- One emulator run saved no path waypoints in a test (`waypoints=0`), so that case did not show a notification. The cause is probably the test data, not the service. The same test passed when run alone.
