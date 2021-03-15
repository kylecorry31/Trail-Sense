package com.kylecorry.trail_sense.tools.tides.domain

import java.time.ZonedDateTime

data class Tide(val time: ZonedDateTime, val isHigh: Boolean)

enum class TideType {
    High,
    Low,
    Half
}