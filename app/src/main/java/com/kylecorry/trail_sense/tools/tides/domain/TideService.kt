package com.kylecorry.trail_sense.tools.tides.domain

import android.content.Context
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.optimization.GoldenSearchExtremaFinder
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.science.oceanography.waterlevel.IWaterLevelCalculator
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.tides.domain.range.TideTableRangeCalculator
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideTableWaterLevelCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TideService(private val context: Context) : ITideService {

    private val maxSearchIterations = 10

    private val ocean = OceanographyService()

    private val cache = mutableMapOf<TideTable, IWaterLevelCalculator>()

    override fun getTides(table: TideTable, date: LocalDate, zone: ZoneId): List<Tide> {
        val start = date.atStartOfDay().toZonedDateTime(zone)
        val end = date.plusDays(1).atStartOfDay().toZonedDateTime(zone)
        val waterLevelCalculator = getTableCalculator(table)
        val extremaFinder = GoldenSearchExtremaFinder(30.0, 1.0)
        val tides = ocean.getTides(waterLevelCalculator, start, end, extremaFinder)
        return tides.filter { it.time.toLocalDate() == date }
    }

    private fun LocalDateTime.toZonedDateTime(zone: ZoneId): ZonedDateTime {
        return ZonedDateTime.of(this, zone)
    }

    override fun getWaterLevel(table: TideTable, time: ZonedDateTime): Float {
        return getTableCalculator(table).calculate(time)
    }

    override fun getWaterLevels(table: TideTable, date: LocalDate): List<Reading<Float>> {
        return Time.getReadings(
            date,
            ZoneId.systemDefault(),
            Duration.ofMinutes(10)
        ) {
            getWaterLevel(table, it)
        }
    }

    override fun getRange(table: TideTable): Range<Float> {
        return TideTableRangeCalculator().getRange(table)
    }

    override fun isWithinTideTable(table: TideTable, time: ZonedDateTime): Boolean {
        val sortedTides = table.tides.sortedBy { it.time }
        for (i in 0 until sortedTides.lastIndex) {
            if (sortedTides[i].time <= time && sortedTides[i + 1].time >= time) {
                val period = Duration.between(sortedTides[i].time, sortedTides[i + 1].time)
                val maxPeriod = Time.hours(180 / table.principalFrequency + 3.0)
                return !(sortedTides[i].isHigh == sortedTides[i + 1].isHigh || period > maxPeriod)
            }
        }
        return false
    }

    override fun getCurrentTide(table: TideTable, time: ZonedDateTime): TideType? {
        val next = getNextTide(table, time) ?: return null
        val timeToNextTide = Duration.between(time, next.time)
        val closeToNextTide = timeToNextTide < Duration.ofHours(2)
        val farFromNextTide = timeToNextTide > Duration.ofHours(4)
        val nextIsHigh = next.isHigh
        val nextIsLow = !next.isHigh
        return if ((nextIsHigh && closeToNextTide) || (nextIsLow && farFromNextTide)) {
            TideType.High
        } else if ((nextIsLow && closeToNextTide) || (nextIsHigh && farFromNextTide)) {
            TideType.Low
        } else {
            null
        }
    }

    override fun isRising(table: TideTable, time: ZonedDateTime): Boolean {
        return getNextTide(table, time)?.isHigh ?: false
    }

    private fun getNextTide(table: TideTable, time: ZonedDateTime, iteration: Int = 0): Tide? {
        if (iteration >= maxSearchIterations) {
            return null
        }

        val todayTides = getTides(table, time.toLocalDate())
        val next = todayTides.firstOrNull { it.time >= time }
        return next ?: getNextTide(
            table,
            time.toLocalDate().plusDays(1).atStartOfDay().atZone(time.zone),
            iteration + 1
        )
    }

    private fun getTableCalculator(table: TideTable): IWaterLevelCalculator {
        return cache.getOrPut(table) { TideTableWaterLevelCalculator(context, table) }
    }
}