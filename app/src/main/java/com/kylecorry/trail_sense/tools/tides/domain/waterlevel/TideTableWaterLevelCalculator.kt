package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.time.Time.hours
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.range.TideTableRangeCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class TideTableWaterLevelCalculator(private val table: TideTable) : IWaterLevelCalculator {

    private val range = TideTableRangeCalculator().getRange(table)
    private val tides = table.tides.sortedBy { it.time }.map { populateHeight(it) }
    private val piecewise by lazy { generatePiecewiseCalculator() }

    override fun calculate(time: ZonedDateTime): Float {
        if (tides.isEmpty()) {
            return 0f
        }

        if (tides.size == 1) {
            val tide = tides[0]
            val frequency =
                if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed
            val amplitude = getAmplitude()
            return TideClockWaterLevelCalculator(tide, frequency, amplitude = amplitude, tide.height!! - amplitude).calculate(time)
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
        return first.isHigh == second.isHigh || period > maxPeriod
    }

    private fun getGapCalculators(
        first: Tide,
        second: Tide
    ): List<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>> {
        val calculators = mutableListOf<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>()

        val frequency =
            if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed

        val start = if (first.isHigh == second.isHigh) {
            val nextTime = first.time.plus(hours(180 / frequency.toDouble()))
            val nextHeight = if (first.isHigh) getAverageLow() else getAverageHigh()
            val nextTide = Tide(
                nextTime,
                !first.isHigh,
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
        val amplitude = (if (!tide.isHigh) -1 else 1) * getAmplitude()
        val z0 = tide.height!! - amplitude
        val tideFrequency =
            if (getFrequency() == TideFrequency.Semidiurnal) TideConstituent.M2.speed else TideConstituent.K1.speed
        return TideClockWaterLevelCalculator(
            tide,
            tideFrequency,
            getAmplitude(),
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
        return range.end
    }

    private fun getAverageLow(): Float {
        return range.start
    }

    private fun getHeight(tide: Tide): Float {
        return tide.height ?: (if (tide.isHigh) range.end else range.start)
    }

    private fun populateHeight(tide: Tide): Tide {
        return tide.copy(height = getHeight(tide))
    }

    private fun getAmplitude(): Float {
        val averageHigh = getAverageHigh()
        val averageLow = getAverageLow()
        return (averageHigh - averageLow) / 2
    }

    private fun getFrequency(): TideFrequency {
        return table.frequency
    }

}