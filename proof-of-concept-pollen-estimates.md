# Proof of concept: pollen estimates (issue #3470)

## Question

Can Trail Sense estimate tree, grass, and weed pollen seasons offline with GDD, using only the monthly min/max temperature data it already has?

## Answer

Yes, with limits. The method works. The thresholds in the issue do not: they put the seasons 3 to 6 weeks late. Thresholds fitted to approximate US allergy calendars give plausible seasons for most mid-latitude places. Weeds, warm temperate zones, and the tropics need more work.

## Changes

### `tools/climate/domain/PollenService.kt` (new)

- `PollenType`: Tree, Grass, Weed. Each has a base temperature and a start and end GDD threshold (in °F-days).
- `PollenService.getYearlyPollenPeriods(year, location, elevation, calibrated)` returns the active date ranges for each type.
- How it works:
  1. Get the daily min/max temperatures for the year from `IWeatherSubsystem.getTemperatureRanges`.
  2. Find the coldest day (lowest daily low). The GDD count resets on that day.
  3. Count GDD per day with `Ecology.getGrowingDegreeDays`. The daily max is capped at 30 °C. The count starts one year early so the first days of the year are correct.
  4. A day is active when cumulative GDD is between the start and end thresholds **and** that day's GDD is above zero.
  5. Join consecutive active days into ranges.
- The "daily GDD above zero" rule is needed. Without it, a type whose end threshold is never reached (weeds in Chicago) stays active until the next winter low.
- The existing `CumulativeGrowingDegreeDaysTrigger` was not used. It resets after any day with zero GDD and cannot restart at the winter low, so it does not fit pollen.

### `tools/climate/ui/ClimateFragment.kt`

- Runs `PollenService` for the selected year, location, and elevation.
- Adds one line per pollen type to the existing ecology text (for example "Tree pollen: Active April to May"). The click dialog shows early/mid/late month.
- The label is a hardcoded English string, not a resource.

## Thresholds used (fitted, not sourced)

| Type | Base temp | Start GDD | End GDD |
|------|-----------|-----------|---------|
| Tree | 40 °F | 20 | 750 |
| Grass | 50 °F | 200 | 1400 |
| Weed | 60 °F | 780 | 1080 |

The issue's values were: trees 50 °F, 100–200; grass 60 °F, 400–800; weeds 60 °F, 1000–1500.

### How they were fitted

- I used approximate season dates from US allergy calendars, written from memory and not cited. Cities: Chicago, Minneapolis, Atlanta, Seattle, Denver.
- For each base temperature (32 to 60 °F), I read the cumulative GDD at each city's start and end date, then compared the spread between cities.
- Excluding Atlanta, the four other cities agree within about 10% for trees and weeds at the chosen bases. Grass is looser (about 30%).
- The fit is circular for these five cities. London, Melbourne, Sydney, Phoenix, Miami, Anchorage, and Quito are the only out-of-sample checks, and they are only judged by eye.

## Example output (2025, fitted thresholds)

| Place | Tree | Grass | Weed |
|-------|------|-------|------|
| Chicago | Apr 2 – May 30 | May 30 – Jul 30 | Aug 15 – Sep 16 |
| Minneapolis | Apr 11 – May 30 | May 26 – Jul 26 | Aug 11 – Sep 22 |
| Denver | Mar 21 – May 28 | May 31 – Jul 30 | Aug 15 – Sep 23 |
| London | Mar 6 – May 27 | Jun 10 – Sep 17 | none |
| Melbourne | Jul 27 – Sep 24 | Oct 5 – Dec 31 (and Jan 1–2) | Feb 12 – Mar 14 |
| Atlanta | Feb 2 – Apr 7 | Apr 9 – Jun 11 | Jun 27 – Jul 12 (wrong; real is Aug–Oct) |
| Miami | Jan 30 – Feb 21 | Feb 8 – Mar 31 | Apr 1 – Apr 18 (wrong) |
| Quito | meaningless (no winter low) | | |

Run time: 40 to 220 ms per location on the emulator (first call includes asset loading).

## Known gaps

- Thresholds are one set for all climates. The data suggests they differ by climate zone (Atlanta did not fit).
- Weed (ragweed) flowering depends on day length, not only GDD. A photoperiod trigger (Sol has `PhotoperiodTrigger`) is likely needed.
- Tropical climates have no winter low. Exclude them, as `SpeciesPhenology` does for some climates.
- Only an active window is estimated. There is no peak or level, so "current level" on the weather tool is not covered.
- The monthly-mean temperatures smooth out daily swings, which likely understates GDD in cool spring months.
- Not built: pollen charts on the climate tool, pollen status on the weather tool, multiple tree and weed species, string resources, tests.
- The climate text lines were compiled but not checked on screen.
- Reference season dates need real sources before the thresholds are trusted.
