package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun <T : Comparable<T>> List<Range<T>>.mergeIntersecting(): List<Range<T>> {
    val newRanges = mutableListOf<Range<T>>()
    for (range in sortedBy { it.start }) {
        if (newRanges.isEmpty()) {
            newRanges.add(range)
        } else {
            val previous = newRanges.last()
            if (previous.contains(range.start)) {
                newRanges.removeAt(newRanges.lastIndex)
                newRanges.add(Range(previous.start, maxOf(previous.end, range.end)))
            } else {
                newRanges.add(range)
            }
        }
    }
    return newRanges
}


inline fun <T> Time.getReadings2(
    date: LocalDate,
    zone: ZoneId,
    step: Duration,
    alwaysIncludeEndOfDay: Boolean = true,
    valueFn: (time: ZonedDateTime) -> T
): List<Reading<T>> {
    return getReadings2(
        date.atStartOfDay().atZone(zone),
        date.atEndOfDay().atZone(zone),
        step,
        alwaysIncludeEndOfDay,
        valueFn
    )
}

inline fun <T> Time.getReadings2(
    start: ZonedDateTime,
    end: ZonedDateTime,
    step: Duration,
    alwaysIncludeEnd: Boolean = true,
    valueFn: (time: ZonedDateTime) -> T
): List<Reading<T>> {

    if (step.isZero || step.isNegative) {
        return emptyList()
    }

    val readings = mutableListOf<Reading<T>>()
    var time = start
    var hasRecordedEnd = false
    while (time <= end) {
        readings.add(Reading(valueFn(time), time.toInstant()))
        if (time == end) {
            hasRecordedEnd = true
        }
        time = time.plus(step)
    }

    if (!hasRecordedEnd && alwaysIncludeEnd) {
        readings.add(Reading(valueFn(end), end.toInstant()))
    }

    return readings
}
