package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.shared.hoursBetween
import com.kylecorry.trail_sense.tools.tides.domain.WaveMath
import java.time.ZonedDateTime

class RuleOfTwelfthsWaterLevelCalculator(
    private val first: Tide,
    private val second: Tide,
    private val approximateFrequency: Float? = null
) :
    IWaterLevelCalculator {

    private val wave by lazy {
        val firstVec = Vector2(getX(first.time), first.height)
        val secondVec = Vector2(getX(second.time), second.height)
        if (approximateFrequency == null) {
            WaveMath.connect(firstVec, secondVec)
        } else {
            WaveMath.connect(firstVec, secondVec, approximateFrequency)
        }
    }

    override fun calculate(time: ZonedDateTime): Float {
        return wave.cosine(getX(time))
    }

    private fun getX(time: ZonedDateTime): Float {
        return hoursBetween(first.time, time)
    }

}