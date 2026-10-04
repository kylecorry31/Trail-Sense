# Deep Sleep

Class: `PowerService`
Method: `getDeepSleep`
Inputs: List of `BatteryReading`
Output: A `DeepSleep` class holding the sleep percent and total elapsed time

```
readings = battery readings with non-null uptime and elapsed time, sorted by time ascending
total sleep = 0
total elapsed = 0

for reading in readings
    delta elapsed = reading elapsed - previous reading elapsed
    delta uptime = reading uptime - previous reading uptime
    delta time = reading.time - previous reading time

    if delta elapsed <= 0 or delta uptime < 0 or |delta elapsed - delta time| > 5 minutes
        skip this reading
    
    total elapsed += delta elapsed
    total sleep += (delta elapsed - delta uptime) clamped to [0, delta elapsed]

if total elapsed = 0
    return null

sleep percent = 100 * total sleep / total elapsed

return sleep percent and total elapsed
```