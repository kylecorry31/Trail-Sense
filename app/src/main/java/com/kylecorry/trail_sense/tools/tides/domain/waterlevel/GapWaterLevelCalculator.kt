package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.Waves
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.waterlevel.IWaterLevelCalculator
import com.kylecorry.sol.time.Time
import java.time.ZonedDateTime

class GapWaterLevelCalculator(
    private val first: Tide,
    private val second: Tide,
    private val approximateFrequency: Float
) :
    IWaterLevelCalculator {

    private val wave by lazy {
        val firstVec = Vector2(getX(first.time), first.height ?: (if (first.isHigh) 1f else -1f))
        val secondVec =
            Vector2(getX(second.time), second.height ?: (if (second.isHigh) 1f else -1f))
        Waves.connect(firstVec, secondVec, approximateFrequency)
    }

    override fun calculate(time: ZonedDateTime): Float {
        return wave.calculate(getX(time))
    }

    private fun getX(time: ZonedDateTime): Float {
        return Time.hoursBetween(first.time, time)
    }

}