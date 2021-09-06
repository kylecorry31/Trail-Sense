package com.kylecorry.trail_sense.shared.sensors

import java.time.ZonedDateTime

interface ITimeProvider {
    fun getTime(): ZonedDateTime
}