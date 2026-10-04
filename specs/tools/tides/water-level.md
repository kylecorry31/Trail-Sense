# Tide Table Water Level

Estimates the water level at any time from a `TideTable`. Heights are in the table's own unit.

Terms:
- tide: a time, high or low, and an optional height
- `range`: the lowest and highest level of the table
- `principalFrequency`: the table's main tidal speed, `M2` for a semidiurnal table and half of that otherwise
- half period: `180 / principalFrequency` hours, the usual time from one tide to the next opposite one
- gap: two consecutive tides that are the same type or more than a half period + 3 hours apart
- estimate: a calculator that predicts levels without the table, used where the table has no data. The first available of harmonic, lunitidal, then clock

Level calculators for the rule of twelfths, harmonics, lunitidal intervals and the tide clock come from Sol and are not specified here.

## Range

- Class: `TideTableRangeCalculator`
- Method: `getRange(table)`
- Output: `Range<Float>`

```
lowest = smallest low tide height
highest = largest high tide height

if lowest is missing
    lowest = (highest, or 1 if missing) - 1
if highest is missing
    highest = lowest + 1

return lowest to highest
```

## Table water level

- Class: `TideTableWaterLevelCalculator(context, table)`
- Method: `calculateSuspend(time)`, with a blocking `calculate(time)`
- Output: the water level, or 0 if it cannot be determined
- Property: `location`, the table's location, replaced by the tide model's location (null if no model is found) whenever a tide model is looked up

```
tides = table's tides sorted by time, with a missing height set to range.highest if high or range.lowest if low

if tides is empty
    if estimator is Harmonic or TideModel
        return harmonic estimate at time
    if estimator is LunitidalInterval and the interval is set
        return lunitidal estimate at time
    return 0

if time is outside the years 2000 to 3000
    return 0

if time is before the first tide, return levelBeforeFirst(time)
if time is after the last tide, return levelAfterLast(time)
return levelBetween(the tides on either side of time)
```

At the first tide the before section is used and at the last tide the after section is used. Otherwise, the earlier section is used at a shared boundary. `PiecewiseWaterLevelCalculator` picks the first of a list of time ranges (ends included) that contains the time, or 0 if there is none.

### Between two tides

```
levelBetween(first, second):
    if not a gap
        return rule of twelfths from first to second

    if first and second are opposite types
        return sine wave through first and second

    opposite = a tide of the opposite type, one half period after first,
               at range.lowest if first is high or range.highest if low
    return rule of twelfths from first to opposite, then sine wave from opposite to second
```

The sine wave is a `GapWaterLevelCalculator`, built with Sol's `Trigonometry.connect` using `principalFrequency`, in radians per hour, as the approximate frequency.

### Before the first tide and after the last tide

`levelBeforeFirst` is shown. `levelAfterLast` is the mirror image, using the estimate's next extrema after the last tide instead of the previous ones before the first.

```
levelBeforeFirst(time):
    estimate = estimate built from the first tide
    extrema = estimate's extrema in the 2 days before the first tide

    if the latest extremum of the first tide's type has the same time and height as the first tide
        return estimate at time

    opposite = latest extremum of the opposite type, or the first tide if there is none

    if time is at or before opposite
        return estimate at time
    return rule of twelfths from opposite to first tide at time
```

### Estimates

```
harmonic (Harmonic and TideModel tables only):
    harmonics = table's harmonics, or for TideModel the model's for the table's location (current location if none)
    no estimate if there are no harmonics

lunitidal (LunitidalInterval, semidiurnal tables only):
    if the table has an interval
        interval = table's interval (at UTC if lunitidalIntervalIsUtc)
    else
        interval = mean interval of the table's high tides and of its low tides
        no estimate if there are no high tides

clock:
    always available
    oscillates at principalFrequency with half the range as its amplitude
    peaks at the tide's time and height if it is high, troughs there if low
    mean = tide's height - amplitude if high, tide's height + amplitude if low
```

A table with no location uses the equator and prime meridian for lunitidal intervals.
