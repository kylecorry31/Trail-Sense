# Deep Sleep

- Class: `PowerService`
- Method: `getDeepSleep`
- Inputs: `batteryReadings` (list of `BatteryReading`)
- Output: A `DeepSleep` class holding the sleep percent and total elapsed time, or null if there is no usable interval

```
readings = batteryReadings with non-null uptime and elapsedRealtime, sorted by time ascending
totalSleep = 0
totalElapsed = 0

for each consecutive pair (previousReading, reading) in readings
    deltaElapsed = reading.elapsedRealtime - previousReading.elapsedRealtime
    deltaUptime = reading.uptime - previousReading.uptime
    deltaTime = reading.time - previousReading.time

    if deltaElapsed <= 0 or deltaUptime < 0 or |deltaElapsed - deltaTime| > 5 minutes
        continue

    totalElapsed += deltaElapsed
    totalSleep += (deltaElapsed - deltaUptime) clamped to [0, deltaElapsed]

if totalElapsed = 0
    return null

sleepPercent = 100 * totalSleep / totalElapsed

return sleepPercent and totalElapsed
```
