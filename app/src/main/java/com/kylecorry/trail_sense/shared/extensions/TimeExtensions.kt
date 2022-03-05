package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.units.Reading
import java.time.*

fun ZonedDateTime.roundNearestMinute(minutes: Long): ZonedDateTime {
    val minute = this.minute
    val newMinute = (minute / minutes) * minutes

    val diff = newMinute - minute
    return this.plusMinutes(diff)
}

fun Instant.hoursUntil(other: Instant): Float {
    return Duration.between(this, other).seconds / (60f * 60f)
}

fun LocalDate.atEndOfDay(): LocalDateTime {
    return atTime(LocalTime.MAX)
}

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

fun <T> getReadings(
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