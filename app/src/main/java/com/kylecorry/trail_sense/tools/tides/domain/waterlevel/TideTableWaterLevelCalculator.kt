package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.Wave
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

class TideTableWaterLevelCalculator(table: TideTable) : IWaterLevelCalculator {

    private val tides = table.tides.sortedBy { it.time }

    override fun calculate(time: ZonedDateTime): Float {
        if (tides.isEmpty()) {
            return 0f
        }

        if (tides.size == 1) {
            val tide = tides[0]
            // TODO: Let the user specify if this should be diurnal or semidiurnal
            return TideClockWaterLevelCalculator(tide).calculate(time)
        }

        val t = Duration.between(tides[0].time, time).seconds / 3600f
        return getWave(time).cosine(t)
    }

    private fun getWave(time: ZonedDateTime): Wave {
        var idx = -1

        for (i in 0 until tides.lastIndex) {
            if (tides[i].time <= time && tides[i + 1].time >= time) {
                idx = i
                break
            }
        }

        return if (idx == -1) {
            if (time <= tides[0].time) {
                getComputedWaveFrom(tides.first())
            } else {
                getComputedWaveFrom(tides.last())
            }
        } else {
            getWaveBetween(
                tides[idx],
                tides[idx + 1]
            )
        }
    }

    private fun getAverageAmplitude(): Float {
        val highs = tides.filter { it.type == TideType.High }
        val lows = tides.filter { it.type == TideType.Low }
        val averageHigh = if (highs.isEmpty()) 0.0 else highs.sumOf { it.height.toDouble() } / highs.size
        val averageLow = if (lows.isEmpty()) 0.0 else lows.sumOf { it.height.toDouble() } / lows.size
        return (averageHigh - averageLow).toFloat() / 2
    }

    private fun isSemidiurnal(): Boolean {
        var averageFrequency = 0f
        var count = 0
        for (i in 0 until tides.lastIndex) {
            // TODO: Check for gap before summing them up
            val period = Duration.between(tides[0].time, tides[1].time).seconds / 3600f
            val frequency = (360 / (2 * period)).toRadians()
            averageFrequency += frequency
            count++
        }
        averageFrequency /= count

        return (TideConstituent.M2.speed.toRadians() - averageFrequency).absoluteValue <= (TideConstituent.K1.speed.toRadians() - averageFrequency).absoluteValue
    }

    private fun getComputedWaveFrom(tide: Tide): Wave {
        // TODO: Use the table to generate two waves: high to low and low to high
        val amplitude = (if (tide.type == TideType.Low) -1 else 1) * getAverageAmplitude()
        val z0 = tide.height - amplitude
        val frequency = if (isSemidiurnal()) TideConstituent.M2.speed else TideConstituent.K1.speed
        val t = Duration.between(tides[0].time, tide.time).seconds / 3600f
        return Wave(
            amplitude,
            frequency.toRadians(),
            t,
            z0
        )
    }

    private fun getWaveBetween(tide1: Tide, tide2: Tide): Wave {
        // TODO: If tides are far apart, use a computed wave
        val period = Duration.between(tide1.time, tide2.time).seconds / 3600f
        val deltaHeight = abs(tide1.height - tide2.height)
        val verticalShift = deltaHeight / 2 + min(tide1.height, tide2.height)
        val frequency = (360 / (2 * period)).toRadians()
        val amplitude = (if (tide1.type == TideType.High) 1 else -1) * deltaHeight / 2
        val t = Duration.between(tides[0].time, tide1.time).seconds / 3600f
        return Wave(amplitude, frequency, t, verticalShift)
    }
}