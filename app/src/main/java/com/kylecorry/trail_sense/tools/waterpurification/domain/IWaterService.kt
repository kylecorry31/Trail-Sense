package com.kylecorry.trail_sense.tools.waterpurification.domain

import com.kylecorry.sol.units.Distance
import java.time.Duration

interface IWaterService {
    fun getPurificationTime(altitude: Distance?): Duration
}