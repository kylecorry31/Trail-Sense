# Proof of concept: beacon geofence alerts

## Goal

Alert the user when they get close to a beacon. The user turns a geofence on or off for each beacon.
The alert must work in the background, without Google Play Services.

## Approach

Use the Android platform geofence API: `LocationManager.addProximityAlert`.
The OS keeps the fences. When the user enters a fence, the OS sends a broadcast to the app.
The app does not poll for location.

An earlier attempt checked the fence inside the Backtrack service. It was dropped.
Fixed polling at the Backtrack interval (15 minutes) can skip a small fence.

## Changes

| File | Change |
|---|---|
| `tools/beacons/infrastructure/geofence/BeaconGeofenceStore.kt` (new) | Keeps the IDs of beacons with the geofence on. Uses `SessionStateSubsystem` preferences (no database change). |
| `tools/beacons/infrastructure/geofence/BeaconGeofenceRegistrar.kt` (new) | Registers or removes the proximity alert for a beacon, based on the store. Does not register if background location is not granted. |
| `tools/beacons/infrastructure/geofence/BeaconGeofenceReceiver.kt` (new) | Receives the OS broadcast. On entry, shows a notification. |
| `tools/beacons/services/BeaconGeofenceToolService.kt` (new) | Tool service. Registers all fences on every `restart()`. |
| `tools/beacons/BeaconsToolRegistration.kt` | Adds the notification channel, the service ID, and the service. |
| `tools/beacons/infrastructure/persistence/BeaconService.kt` | `delete(beacon)` removes the alert and the stored ID. |
| `tools/beacons/ui/PlaceBeaconFragment.kt` | Loads the toggle from the store. On save, stores the toggle and registers or removes the alert. |
| `tools/beacons/ui/form/CreateBeaconData.kt`, `CreateBeaconForm.kt` | New `geofenceEnabled` form field, bound to the switch. It is not part of `Beacon`. |
| `res/layout/fragment_create_beacon.xml`, `res/values/strings.xml` | "Alert when nearby" switch. |
| `AndroidManifest.xml` | Declares `BeaconGeofenceReceiver`. |

No new permissions. `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION` are already declared.

## How it works

1. `TrailSenseServiceUtils.restartServices` runs on app start and on boot. It calls `BeaconGeofenceToolService.restart()`.
2. The registrar loads all beacons. For each non-temporary beacon in the store, it calls `addProximityAlert`.
3. Each alert uses a `PendingIntent` for `BeaconGeofenceReceiver`.
   - The `PendingIntent` must be `FLAG_MUTABLE`. The system adds the entering/exiting extra to it.
   - The intent data is `trailsense://beacon-geofence/<beacon id>`. This makes each `PendingIntent` unique.
   - The beacon name is an intent extra. The receiver does not need a database lookup.
4. When the user enters a fence, the OS sends the broadcast. The receiver shows the notification.

The toggle is on the create/edit beacon screen. When the user saves, the store and the OS alert are updated together.

## Results (emulator, API 36, with Play Services)

- The fence is registered. The OS lists `Geofence[(40.02, -105.27) 100.0m]` for the app.
- The registration survives `am kill`.
- A location inside the fence gives the notification about 5 seconds after the app starts.
- Without background location, no fence is registered. With it, one fence is registered.
- `force-stop` removes the fences. They return on the next app start or boot.

## Not confirmed

- **The per-beacon toggle.** It builds, but it was not run on the emulator. Another process took the emulator before the test.
  - The results above are from the earlier version, which registered every beacon.

- **Waking a killed app.** With the app process dead, the OS did not check the fence in about 5 minutes.
  - The OS geofence request interval was 30 minutes while the device was about 1 km from the fence.
  - The OS received locations only while the app was in the foreground and requesting GPS.
  - Detection latency for a killed app can be up to 30 minutes on this emulator.
  - This is the main risk. It needs a real device test, ideally without Play Services.
- Behavior with Doze and manufacturer battery limits.
- Accuracy of the entry time on real hardware.

## Hardcoded or stubbed

- The geofence radius is 100 m for every beacon. There is no radius setting.
- The toggle state is in session state preferences, not in the beacon database. It is lost if the app data is cleared. It is not included in beacon export, import, or backup.
- Beacons created by other paths (import, quick action, map) have the geofence off. Beacons deleted outside `BeaconService.delete` keep their stored ID.
- Alerts fire on entry only. There is no exit alert.
- The service is always enabled. There is no on/off setting.
- Notification text, channel name, and service name are hardcoded English strings.
- The user is not asked for background location. The registrar skips registration without it.
- `addProximityAlert` is deprecated since API 29. It still works on API 36.

## Work for a full implementation

- Store the geofence setting (and a radius) on the beacon: add a Room column and a migration, and add it to export and import.
- Register and unregister a fence when a beacon changes by any path (use `BROADCAST_BEACONS_CHANGED`).
- Add a setting to turn alerts on and off. Use the setting for `isEnabled()`.
- Ask for background location when the user turns on a geofence.
- Use string resources and add a user guide entry.
- Decide about the deprecated API. Check behavior on devices without Play Services.
