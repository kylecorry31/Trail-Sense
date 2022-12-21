package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun <T> getReadings(
    date: LocalDate,
    zone: ZoneId,
    step: Duration,
    valueFn: (time: ZonedDateTime) -> T
): List<Reading<T>> {
    return getReadings(
        date.atStartOfDay().atZone(zone),
        date.atEndOfDay().atZone(zone),
        step,
        valueFn
    )
}

inline fun <T> getReadings(
    start: ZonedDateTime,
    end: ZonedDateTime,
    step: Duration,
    valueFn: (time: ZonedDateTime) -> T
): List<Reading<T>> {
    val readings = mutableListOf<Reading<T>>()
    var time = start
    while (time <= end) {
        readings.add(Reading(valueFn(time), time.toInstant()))
        time = time.plus(step)
    }
    return readings
}

fun List<Reading<*>>.duration(): Duration {
    val start = minByOrNull { it.time } ?: return Duration.ZERO
    val end = maxByOrNull { it.time } ?: return Duration.ZERO
    return Duration.between(start.time, end.time)
}