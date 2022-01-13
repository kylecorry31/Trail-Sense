package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
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
        return if (tides.isEmpty()) 0f else piecewise.calculate(time)
    }

    private fun generatePiecewiseCalculator(): IWaterLevelCalculator {
        val calculators = mutableListOf(
            Range(MIN_TIME, tides.first().time) to getBeforeCalculator(),
            Range(tides.last().time, MAX_TIME) to getAfterCalculator()
        )

        val tableCalculators = tides.zipWithNext().map {
            if (hasGap(it.first, it.second)){
               getGapCalculator(it.first, it.second)
            } else {
                val range = Range(it.first.time, it.second.time)
                range to RuleOfTwelfthsWaterLevelCalculator(it.first, it.second)
            }
        }

        calculators.addAll(tableCalculators)

        return PiecewiseWaterLevelCalculator(calculators)
    }

    private fun hasGap(first: Tide, second: Tide): Boolean {
        val period = Duration.between(first.time, second.time)
        val frequency = getMainConstituent().speed
        val maxPeriod = hours(180 / frequency.toDouble() + 3.0)
        return first.isHigh == second.isHigh || period > maxPeriod
    }

    private fun getGapCalculator(
        first: Tide,
        second: Tide
    ): Pair<Range<ZonedDateTime>, IWaterLevelCalculator> {
        val calculators = mutableListOf<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>()

        val frequency = getMainConstituent().speed

        val start = if (first.isHigh == second.isHigh) {
            val nextTime = first.time.plus(hours(180 / frequency.toDouble()))
            val nextHeight = if (first.isHigh) range.start else range.end
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


        return Range(first.time, second.time) to PiecewiseWaterLevelCalculator(calculators)
    }

    private fun getPastFutureCalculator(tide: Tide): IWaterLevelCalculator {
        val amplitude = (if (!tide.isHigh) -1 else 1) * getAmplitude()
        val z0 = tide.height!! - amplitude
        return TideClockWaterLevelCalculator(
            tide,
            getMainConstituent().speed,
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

    private fun getHeight(tide: Tide): Float {
        return tide.height ?: (if (tide.isHigh) range.end else range.start)
    }

    private fun populateHeight(tide: Tide): Tide {
        return tide.copy(height = getHeight(tide))
    }

    private fun getAmplitude(): Float {
        return (range.end - range.start) / 2
    }

    private fun getMainConstituent(): TideConstituent {
        return if (table.isSemidiurnal) {
            TideConstituent.M2
        } else {
            TideConstituent.K1
        }
    }

    companion object {
        private val MIN_TIME = LocalDate.of(2000, 1, 1).atStartOfDay().toZonedDateTime()
        private val MAX_TIME = LocalDate.of(3000, 1, 1).atStartOfDay().toZonedDateTime()
    }

}