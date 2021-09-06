package com.kylecorry.trail_sense.shared.sensors

import java.time.ZonedDateTime

class SystemTimeProvider : ITimeProvider {
    override fun getTime(): ZonedDateTime {
        return ZonedDateTime.now()
    }
}