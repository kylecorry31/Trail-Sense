package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.waterlevel.HarmonicWaterLevelCalculator
import com.kylecorry.sol.science.oceanography.waterlevel.IWaterLevelCalculator
import com.kylecorry.sol.science.oceanography.waterlevel.RuleOfTwelfthsWaterLevelCalculator
import com.kylecorry.sol.science.oceanography.waterlevel.TideClockWaterLevelCalculator
import com.kylecorry.sol.time.Time.hours
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.range.TideTableRangeCalculator
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.TideModel
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class TideTableWaterLevelCalculator(private val context: Context, private val table: TideTable) :
    IWaterLevelCalculator {
    private val range = TideTableRangeCalculator().getRange(table)
    private val tides = table.tides.sortedBy { it.time }.map { populateHeight(it) }
    private val piecewise by lazy { generatePiecewiseCalculator() }
    private val ocean = OceanographyService()
    private val extremaFinder = GoldenSearchExtremaFinder(30.0, 1.0)
    private val locationSubsystem = LocationSubsystem.getInstance(context)
    private val harmonic by lazy { getHarmonicCalculator() }
    private val lunitidal by lazy { getLunitidalCalculator() }

    override fun calculate(time: ZonedDateTime): Float {
        // Harmonic tides don't require a table
        if (tides.isEmpty() && (table.estimator == TideEstimator.Harmonic || table.estimator == TideEstimator.TideModel)) {
            return harmonic?.calculate(time) ?: 0f
        }

        // Lunitidal tides don't require a table if the interval is specified
        if (tides.isEmpty() && table.estimator == TideEstimator.LunitidalInterval && table.lunitidalInterval != null) {
            return lunitidal?.calculate(time) ?: 0f
        }

        return if (tides.isEmpty()) 0f else piecewise.calculate(time)
    }

    private fun generatePiecewiseCalculator(): IWaterLevelCalculator {
        val calculators = mutableListOf(
            Range(MIN_TIME, tides.first().time) to getGapCalculator2(null, tides.first()),
            Range(tides.last().time, MAX_TIME) to getGapCalculator2(tides.last(), null)
        )

        val tableCalculators = tides.zipWithNext().map {
            val range = Range(it.first.time, it.second.time)
            range to getGapCalculator2(
                it.first,
                it.second
            )
        }

        calculators.addAll(tableCalculators)

        return PiecewiseWaterLevelCalculator(calculators)
    }

    private fun hasGap(first: Tide, second: Tide): Boolean {
        val period = Duration.between(first.time, second.time)
        val frequency = table.principalFrequency
        val maxPeriod = hours(180 / frequency.toDouble() + 3.0)
        return first.isHigh == second.isHigh || period > maxPeriod
    }

    private fun getGapCalculator2(
        first: Tide?,
        second: Tide?
    ): IWaterLevelCalculator {
        // If both are null, it should use the main calculator
        if (first == null && second == null) {
            return PiecewiseWaterLevelCalculator(listOf())
        }

        // The start is null
        if (first == null && second != null) {
            val estimateCalculator = getEstimateCalculator(second)
            // First check to see if it lines up with the tide table
            val lastTideBefore = getLastTideBefore(estimateCalculator, second.time, second.isHigh)
            if (lastTideBefore?.time == second.time && lastTideBefore.height == second.height) {
                return estimateCalculator
            }

            // Otherwise, a gap needs to be filled
            val lastOtherTideBefore =
                getLastTideBefore(estimateCalculator, second.time, !second.isHigh)
            val gapCalculator = RuleOfTwelfthsWaterLevelCalculator(
                lastOtherTideBefore ?: second,
                second
            )
            return PiecewiseWaterLevelCalculator(
                listOf(
                    Range(MIN_TIME, lastOtherTideBefore?.time ?: second.time) to estimateCalculator,
                    Range(lastOtherTideBefore?.time ?: second.time, second.time) to gapCalculator
                )
            )
        }

        // The end is null
        if (first != null && second == null) {
            val estimateCalculator = getEstimateCalculator(first)
            // First check to see if it lines up with the tide table
            val nextTideAfter = getNextTideAfter(estimateCalculator, first.time, first.isHigh)
            if (nextTideAfter?.time == first.time && nextTideAfter.height == first.height) {
                return estimateCalculator
            }

            // Otherwise, a gap needs to be filled
            val nextOtherTideAfter = getNextTideAfter(estimateCalculator, first.time, !first.isHigh)
            val gapCalculator = RuleOfTwelfthsWaterLevelCalculator(
                first,
                nextOtherTideAfter ?: first
            )
            return PiecewiseWaterLevelCalculator(
                listOf(
                    Range(first.time, nextOtherTideAfter?.time ?: first.time) to gapCalculator,
                    Range(nextOtherTideAfter?.time ?: first.time, MAX_TIME) to estimateCalculator
                )
            )
        }

        // There may be a gap between the two tides
        if (!hasGap(first!!, second!!)) {
            return RuleOfTwelfthsWaterLevelCalculator(first, second)
        }

        // Fill either end of the gap if needed
        // TODO: This should use the same gap filling logic as above
        return getGapCalculator(first, second).second
    }

    private fun getEstimateCalculator(referenceTide: Tide): IWaterLevelCalculator {
        return harmonic ?: lunitidal ?: getClockCalculator(
            referenceTide
        )
    }

    private fun getLastTideBefore(
        calculator: IWaterLevelCalculator,
        time: ZonedDateTime,
        isHigh: Boolean
    ): Tide? {
        val tides = getTides(calculator, time.minusDays(2), time)
        return tides.filter { it.isHigh == isHigh }.maxByOrNull { it.time }
    }

    private fun getNextTideAfter(
        calculator: IWaterLevelCalculator,
        time: ZonedDateTime,
        isHigh: Boolean
    ): Tide? {
        val tides = getTides(calculator, time, time.plusDays(2))
        return tides.filter { it.isHigh == isHigh }.minByOrNull { it.time }
    }

    private fun getTides(
        calculator: IWaterLevelCalculator,
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<Tide> {
        return ocean.getTides(calculator, start, end, extremaFinder)
    }

    private fun getLunitidalCalculator(): IWaterLevelCalculator? {
        if (table.estimator != TideEstimator.LunitidalInterval || !table.isSemidiurnal) {
            return null
        }

        // If the lunitidal interval is specified, use it
        if (table.lunitidalInterval != null) {
            return LunitidalWaterLevelCalculator(
                table.lunitidalInterval,
                if (table.lunitidalIntervalIsUtc) Coordinate.zero else table.location
                    ?: Coordinate.zero,
                null,
                range
            )
        }

        // Otherwise, calculate it
        if (!tides.any { it.isHigh }) {
            return null
        }

        val highTides = tides.filter { it.isHigh }
        val lowTides = tides.filter { !it.isHigh }

        val highInterval = ocean.getMeanLunitidalInterval(
            highTides.map { it.time },
            table.location ?: Coordinate.zero
        ) ?: return null
        val lowInterval = ocean.getMeanLunitidalInterval(
            lowTides.map { it.time },
            table.location ?: Coordinate.zero
        )
        return LunitidalWaterLevelCalculator(
            highInterval,
            table.location ?: Coordinate.zero,
            lowInterval,
            range
        )
    }

    private fun getHarmonicCalculator(): IWaterLevelCalculator? {
        if (table.estimator != TideEstimator.Harmonic && table.estimator != TideEstimator.TideModel) {
            return null
        }

        val harmonics = if (table.estimator == TideEstimator.Harmonic) {
            table.harmonics
        } else {
            runBlocking {
                TideModel.getHarmonics(
                    context,
                    table.location ?: locationSubsystem.location
                )
            }
        }

        if (harmonics.isEmpty()) {
            return null
        }

        return HarmonicWaterLevelCalculator(harmonics)
    }

    private fun getClockCalculator(tide: Tide): IWaterLevelCalculator {
        val amplitude = (if (!tide.isHigh) -1 else 1) * getAmplitude()
        val z0 = tide.height!! - amplitude
        return TideClockWaterLevelCalculator(
            tide,
            table.principalFrequency,
            getAmplitude(),
            z0
        )
    }


    private fun getGapCalculator(
        first: Tide,
        second: Tide
    ): Pair<Range<ZonedDateTime>, IWaterLevelCalculator> {
        val calculators = mutableListOf<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>()

        val frequency = table.principalFrequency

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
            Range(start.time, second.time) to GapWaterLevelCalculator(
                start,
                second,
                frequency.toRadians()
            )
        )


        return Range(first.time, second.time) to PiecewiseWaterLevelCalculator(calculators)
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

    companion object {
        private val MIN_TIME = LocalDate.of(2000, 1, 1).atStartOfDay().toZonedDateTime()
        private val MAX_TIME = LocalDate.of(3000, 1, 1).atStartOfDay().toZonedDateTime()
    }
}