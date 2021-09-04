package com.kylecorry.trail_sense.tools.waterpurification.domain

import com.kylecorry.andromeda.core.units.Distance
import java.time.Duration

interface IWaterService {
    fun getPurificationTime(altitude: Distance?): Duration
}