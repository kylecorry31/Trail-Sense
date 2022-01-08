package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.tools.tides.domain.Wave
import java.time.Duration
import java.time.ZonedDateTime

class TideClockWaterLevelCalculator(private val reference: Tide) : IWaterLevelCalculator {

    private val wave = getWave()

    override fun calculate(time: ZonedDateTime): Float {
        // TODO: Handle if it is diurnal
        val t = Duration.between(reference.time, time).seconds / 3600f
        return wave.cosine(t)
    }

    private fun getWave(): Wave {
        val frequency = TideConstituent.M2.speed
        val amplitude = if (reference.type == TideType.Low) -1f else 1f
        return Wave(amplitude, frequency.toRadians(), 0f, 0f)
    }

}