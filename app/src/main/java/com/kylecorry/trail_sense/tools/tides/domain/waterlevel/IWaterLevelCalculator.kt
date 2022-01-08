package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import java.time.ZonedDateTime

interface IWaterLevelCalculator {
    fun calculate(time: ZonedDateTime): Float
}