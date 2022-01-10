package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class TideTableWaterLevelCalculator(table: TideTable) : IWaterLevelCalculator {

    private val tides = table.tides.sortedBy { it.time }
    private val piecewise by lazy { generatePiecewiseCalculator() }

    override fun calculate(time: ZonedDateTime): Float {
        if (tides.isEmpty()) {
            return 0f
        }

        if (tides.size == 1) {
            val tide = tides[0]
            // TODO: Let the user specify if this should be diurnal or semidiurnal
            return TideClockWaterLevelCalculator(tide).calculate(time)
        }

        return piecewise.calculate(time)
    }

    private fun generatePiecewiseCalculator(): IWaterLevelCalculator {
        val minTime = LocalDate.of(2000, 1, 1).atStartOfDay().toZonedDateTime()
        val maxTime = LocalDate.of(3000, 1, 1).atStartOfDay().toZonedDateTime()

        val calculators = mutableListOf(
            Range(minTime, tides.first().time) to getBeforeCalculator(),
            Range(tides.last().time, maxTime) to getAfterCalculator()
        )

        // TODO: Check for gaps and create a calculator for them
        for (i in 0 until tides.lastIndex) {
            val range = Range(tides[i].time, tides[i + 1].time)
            calculators.add(range to RuleOfTwelfthsWaterLevelCalculator(tides[i], tides[i + 1]))
        }

        return PiecewiseWaterLevelCalculator(calculators)
    }

    private fun getBeforeCalculator(): IWaterLevelCalculator {
        val amplitude = (if (tides.first().type == TideType.Low) -1 else 1) * getAverageAmplitude()
        val z0 = tides.first().height - amplitude
        val tideFrequency = getFrequency()
        return TideClockWaterLevelCalculator(
            tides.first(),
            tideFrequency,
            getAverageAmplitude(),
            z0
        )
    }

    private fun getAfterCalculator(): IWaterLevelCalculator {
        val amplitude = (if (tides.last().type == TideType.Low) -1 else 1) * getAverageAmplitude()
        val z0 = tides.last().height - amplitude
        val tideFrequency = getFrequency()
        return TideClockWaterLevelCalculator(tides.last(), tideFrequency, getAverageAmplitude(), z0)
    }


    private fun getAverageAmplitude(): Float {
        val highs = tides.filter { it.type == TideType.High }
        val lows = tides.filter { it.type == TideType.Low }
        val averageHigh =
            if (highs.isEmpty()) 0.0 else highs.sumOf { it.height.toDouble() } / highs.size
        val averageLow =
            if (lows.isEmpty()) 0.0 else lows.sumOf { it.height.toDouble() } / lows.size
        return (averageHigh - averageLow).toFloat() / 2
    }

    private fun getFrequency(): TideFrequency {
        val periods = mutableListOf<Duration>()
        for (i in 0 until tides.lastIndex) {
            val period = Duration.between(tides[i].time, tides[i + 1].time)
            periods.add(period)
        }
        val semidiurnalThreshold = Duration.ofHours(8)
        val semidiurnal = periods.any { it < semidiurnalThreshold }
        return if (semidiurnal) TideFrequency.Semidiurnal else TideFrequency.Diurnal
    }

}