package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.analysis.CosineWave
import com.kylecorry.sol.math.analysis.Waveform
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.time.Time.hoursBetween
import java.time.ZonedDateTime

class TideClockWaterLevelCalculator(
    private val reference: Tide,
    private val frequency: Float = TideConstituent.M2.speed,
    private val amplitude: Float = 1f,
    private val z0: Float = 0f
) : IWaterLevelCalculator {

    private val wave = getWave()

    override fun calculate(time: ZonedDateTime): Float {
        val t = hoursBetween(reference.time, time)
        return wave.calculate(t)
    }

    private fun getWave(): Waveform {
        val amplitude = if (!reference.isHigh) -amplitude else amplitude
        return CosineWave(amplitude, frequency.toRadians(), 0f, z0)
    }

}