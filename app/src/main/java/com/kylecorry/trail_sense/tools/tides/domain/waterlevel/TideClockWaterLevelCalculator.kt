package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.tools.tides.domain.Wave
import java.time.Duration
import java.time.ZonedDateTime

class TideClockWaterLevelCalculator(
    private val reference: Tide,
    private val frequency: TideFrequency = TideFrequency.Semidiurnal,
    private val amplitude: Float = 1f,
    private val z0: Float = 0f
) : IWaterLevelCalculator {

    private val wave = getWave()

    override fun calculate(time: ZonedDateTime): Float {
        val t = Duration.between(reference.time, time).seconds / 3600f
        return wave.cosine(t)
    }

    private fun getWave(): Wave {
        val frequency =
            if (frequency == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed
        val amplitude = if (reference.type == TideType.Low) -amplitude else amplitude
        return Wave(amplitude, frequency.toRadians(), 0f, z0)
    }

}