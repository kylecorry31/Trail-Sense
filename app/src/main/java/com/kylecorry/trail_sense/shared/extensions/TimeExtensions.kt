package com.kylecorry.trail_sense.shared.extensions

import java.time.ZonedDateTime

fun ZonedDateTime.roundNearestMinute(minutes: Long): ZonedDateTime {
    val minute = this.minute
    val newMinute = (minute / minutes) * minutes

    val diff = newMinute - minute
    return this.plusMinutes(diff)
}