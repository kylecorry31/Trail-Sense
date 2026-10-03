# Weather Forecast

This documents the Sol weather forecast algorithm. It is a rule-based forecast derived from the pressure history and logged clouds.

## Inputs and Output

Inputs:

- **Pressure readings**: Barometer readings over time.
- **Cloud readings**: Logged clouds over time. A reading with no cloud genus means a clear sky.
- **Daily temperature range**: The expected low and high for the next 24 hours (or less), if available.
- **Time**: The time to forecast from.
- **Pressure change threshold**: The pressure change rate (hPa/hr) at which pressure is considered rising or falling.
- **Storm change threshold**: The pressure change rate (hPa/hr) at which a falling pressure is considered a storm.

Output is a list of two forecasts:

1. **Now**: The conditions, front, pressure system, and pressure tendency, along with the time the conditions arrive (if known).
2. **Later**: The conditions and pressure system expected after the current conditions.

Conditions are a combination of clear, overcast, precipitation (with rain or snow, if known), storm, thunderstorm, and wind.

## Preparing the Observations

Only readings from the 48 hours before the forecast time are used, in time order.

Clouds are only used if at least one was logged in the last 24 hours. Otherwise, all clouds are ignored, so the forecast is based on pressure alone.

## Forecast Factors

### Pressure Tendency

The tendency compares the latest pressure reading with the reading closest to 3 hours before it (not including the latest reading itself). The change rate is the pressure difference divided by the time between those two readings.

| Characteristic | Change rate (hPa/hr)                   |
|----------------|----------------------------------------|
| Falling fast   | At most `-(threshold + 2/3)`           |
| Falling        | At most `-threshold`                   |
| Steady         | Between the falling and rising limits  |
| Rising         | At least `threshold`                   |
| Rising fast    | At least `threshold + 2/3`             |

If there are fewer than two pressure readings, or both readings are at the same time, the tendency is steady with no change.

### Pressure System

Based on the latest pressure reading. There is no pressure system if there are no readings.

- **High**: At least 1022.689 hPa.
- **Low**: At most 1009.144 hPa.
- Otherwise, neither.

### Cloud Patterns

The clouds are compared against sequences of cloud groups that indicate a front is approaching. The cloud history must match a pattern exactly to indicate a front. The most recent reading must be in the pattern's last group. Going backward in time, each reading must belong to the current group or the group before it. A reading that doesn't belong (including a clear sky) or a skipped group means there is no match. The oldest reading must be in the pattern's first group.

The cloud groups are:

- **Cirro**: Cirrus, cirrocumulus, cirrostratus.
- **Alto**: Altocumulus, altostratus.
- **Warm**: Stratus, nimbostratus.
- **Cold**: Cumulus, cumulonimbus.

| Pattern                    | Indicates   |
|----------------------------|-------------|
| Cirro, alto, warm          | Warm front  |
| Nimbostratus               | Warm front  |
| Cirro, alto, cold          | Cold front  |
| Cumulonimbus               | Cold front  |
| Cirro, alto                | A front (unknown type) |

### Fronts

A cold front is indicated by the clouds, or by the pressure rising (rising or rising fast). A warm front is indicated by the pressure falling (falling or falling fast) or by the clouds, but only if it is not already a cold front. If neither applies, there is no front.

The forecast also tracks the front indicated by the clouds alone, which is used by the storm and wind predictions.

### Current Cloud

The most recent cloud reading, if it is within 3 hours of the forecast time. Stratus, nimbostratus, stratocumulus, and altostratus are overcast clouds.

## Current Conditions

Each condition is predicted independently, so there can be several at once.

| Condition     | Likely when                                                                                                                                                                                  |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Storm         | The pressure is not rising, and either the clouds indicate a cold front or the pressure is falling by at least the storm threshold (in hPa/hr).                                              |
| Thunderstorm  | A storm is likely, the front is cold, and the daily high is above 55°F (about 12.8°C). Without a daily temperature range, this is never likely.                                              |
| Precipitation | The pressure is not rising, and either there is a warm front or the clouds indicate any front.                                                                                               |
| Wind          | The pressure is changing rapidly (rising fast or falling fast), or the clouds indicate a cold front and the pressure is not rising.                                                          |
| Clear         | If there is a current cloud, and it is a clear sky. If there is no current cloud, the pressure system is high and the pressure is steady or rising. Never if the current cloud is overcast.  |
| Overcast      | If there is a current cloud, and it is an overcast cloud. If there is no current cloud, the pressure system is low and the pressure is steady or falling.                                    |

Some conditions imply others, which are then added:

- A storm adds precipitation and wind.
- Precipitation adds overcast, and a precipitation type if the daily temperature range is known: rain if the low is above 0°C, snow if the high is at most 0°C. If the range spans freezing, no type is added.

## Later Conditions

The later forecast is based on where the weather is heading. It has no front or tendency, and precipitation is never predicted.

| Forecast        | Likely when                                                                                                                                                              |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Clear later     | There is a cold front and the pressure system is not low, or the current cloud is a clear sky or overcast cloud with a high pressure system and steady pressure.         |
| Overcast later  | There is a warm front and the pressure system is not high, or the current cloud is a clear sky or overcast cloud with a low pressure system and steady pressure.         |
| High pressure   | There is a cold front and the pressure system is not low, or the pressure system is high and steady.                                                                     |
| Low pressure    | There is a warm front and the pressure system is not high, or the pressure system is low and steady.                                                                     |

The later pressure system is high if high pressure is likely, otherwise low if low pressure is likely, otherwise none.

## Arrival Time

The arrival time is the time the current conditions are expected to begin, or unknown.

- If there is exactly one condition, and it is wind or a clear or overcast sky with steady pressure, the conditions are already here, so the arrival time is the forecast time.
- If precipitation is not one of the conditions, the arrival time is unknown.
- Otherwise, it is estimated from the clouds. Each cloud type is expected to be followed by precipitation after a delay from when it was logged.

  | Cloud                       | Delay (hours) |
  |-----------------------------|---------------|
  | Cirrus                      | 12 to 24      |
  | Cirrocumulus                | 8 to 12       |
  | Cirrostratus                | 10 to 15      |
  | Altocumulus                 | 0 to 12       |
  | Altostratus                 | 0 to 8        |
  | Stratus, cumulus            | 0 to 3        |
  | Nimbostratus, cumulonimbus  | 0             |
  | Stratocumulus, clear sky    | No estimate   |

  Only the clouds in the first front pattern that matches are used, or all clouds if none matches (a match here does not need to be exact). Starting with the most recent cloud, each delay range (offset by the time the cloud was logged) is intersected with the others, stopping at the first range that doesn't overlap. The arrival time is the middle of the resulting range, or unknown if there is none.

## Missing Current Conditions

If no current conditions are predicted (ex. a steady pressure with no clouds), they are inferred from earlier forecasts. Starting with the most recent pressure or cloud reading before the forecast time, and moving back through the readings, the forecast is recalculated as of each reading's time. This stops once it reaches readings more than 8 hours old. The first earlier forecast with conditions is used, preferring its later conditions over its current ones. Those conditions replace the current conditions of the forecast. Everything else, including the arrival time, is unchanged.
