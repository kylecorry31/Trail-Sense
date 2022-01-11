package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time.hours
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class TideTableWaterLevelCalculator(private val table: TideTable) : IWaterLevelCalculator {

    private val tides = table.tides.sortedBy { it.time }
    private val piecewise by lazy { generatePiecewiseCalculator() }

    override fun calculate(time: ZonedDateTime): Float {
        if (tides.isEmpty()) {
            return 0f
        }

        if (tides.size == 1) {
            val tide = tides[0]
            val frequency =
                if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed
            return TideClockWaterLevelCalculator(tide, frequency).calculate(time)
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

        for (i in 0 until tides.lastIndex) {
            if (hasGap(tides[i], tides[i + 1])) {
                calculators.addAll(getGapCalculators(tides[i], tides[i + 1]))
                continue
            }
            val range = Range(tides[i].time, tides[i + 1].time)
            calculators.add(range to RuleOfTwelfthsWaterLevelCalculator(tides[i], tides[i + 1]))
        }

        return PiecewiseWaterLevelCalculator(calculators)
    }

    private fun hasGap(first: Tide, second: Tide): Boolean {
        val period = Duration.between(first.time, second.time)
        val constituent =
            if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2 else TideConstituent.K1
        val maxPeriod = hours(180 / constituent.speed.toDouble() + 3.0)
        return first.type == second.type || period > maxPeriod
    }

    private fun getGapCalculators(
        first: Tide,
        second: Tide
    ): List<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>> {
        val calculators = mutableListOf<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>()

        val frequency =
            if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed

        val start = if (first.type == second.type) {
            val nextTime = first.time.plus(hours(180 / frequency.toDouble()))
            val nextHeight = if (first.type == TideType.High) getAverageLow() else getAverageHigh()
            val nextTide = Tide(
                nextTime,
                if (first.type == TideType.High) TideType.Low else TideType.High,
                nextHeight
            )
            calculators.add(
                Range(first.time, nextTime) to RuleOfTwelfthsWaterLevelCalculator(
                    first,
                    nextTide
                )
            )
            nextTide
        } else {
            first
        }

        calculators.add(
            Range(start.time, second.time) to RuleOfTwelfthsWaterLevelCalculator(
                start,
                second,
                frequency.toRadians()
            )
        )


        return calculators
    }

    private fun getPastFutureCalculator(tide: Tide): IWaterLevelCalculator {
        val amplitude = (if (tide.type == TideType.Low) -1 else 1) * getAverageAmplitude()
        val z0 = tide.height - amplitude
        val tideFrequency =
            if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed
        return TideClockWaterLevelCalculator(
            tide,
            tideFrequency,
            getAverageAmplitude(),
            z0
        )
    }

    private fun getBeforeCalculator(): IWaterLevelCalculator {
        return getPastFutureCalculator(tides.first())
    }

    private fun getAfterCalculator(): IWaterLevelCalculator {
        return getPastFutureCalculator(tides.last())
    }

    private fun getAverageHigh(): Float {
        val highs = tides.filter { it.type == TideType.High }
        val high = if (highs.isEmpty()) 0.0 else highs.sumOf { it.height.toDouble() } / highs.size
        return high.toFloat()
    }

    private fun getAverageLow(): Float {
        val lows = tides.filter { it.type == TideType.Low }
        val low = if (lows.isEmpty()) 0.0 else lows.sumOf { it.height.toDouble() } / lows.size
        return low.toFloat()
    }

    private fun getAverageAmplitude(): Float {
        val averageHigh = getAverageHigh()
        val averageLow = getAverageLow()
        return (averageHigh - averageLow) / 2
    }

    private fun getFrequency(): TideFrequency {
        return table.frequency
    }

}