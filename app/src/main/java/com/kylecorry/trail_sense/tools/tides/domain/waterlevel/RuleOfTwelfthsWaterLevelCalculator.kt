package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.tools.tides.domain.WaveMath
import java.time.Duration
import java.time.ZonedDateTime

class RuleOfTwelfthsWaterLevelCalculator(private val first: Tide, private val second: Tide) :
    IWaterLevelCalculator {

    private val wave by lazy {
        val firstVec = Vector2(getX(first.time), first.height)
        val secondVec = Vector2(getX(second.time), second.height)
        WaveMath.connect(firstVec, secondVec)
    }

    override fun calculate(time: ZonedDateTime): Float {
        return wave.cosine(getX(time))
    }

    private fun getX(time: ZonedDateTime): Float {
        return Duration.between(first.time, time).seconds / 3600f
    }

}