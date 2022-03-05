package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.extensions.getReadings
import com.kylecorry.trail_sense.tools.tides.domain.range.TideTableRangeCalculator
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideTableWaterLevelCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TideService {

    private val ocean = OceanographyService()

    fun getTides(table: TideTable, date: LocalDate): List<Tide> {
        val start = date.atStartOfDay().toZonedDateTime()
        val end = date.plusDays(1).atStartOfDay().toZonedDateTime()
        val waterLevelCalculator = TideTableWaterLevelCalculator(table)
        val extremaFinder = SimpleExtremaFinder(1.0)
        return ocean.getTides(waterLevelCalculator, start, end, extremaFinder)
    }

    fun getWaterLevel(table: TideTable, time: ZonedDateTime): Float {
        val strategy = TideTableWaterLevelCalculator(table)
        return strategy.calculate(time)
    }

    fun getWaterLevels(table: TideTable, date: LocalDate): List<Reading<Float>> {
        return getReadings(
            date,
            ZoneId.systemDefault(),
            Duration.ofMinutes(10)
        ) {
            getWaterLevel(table, it)
        }
    }

    fun getRange(table: TideTable): Range<Float> {
        return TideTableRangeCalculator().getRange(table)
    }

    fun isWithinTideTable(table: TideTable, time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val sortedTides = table.tides.sortedBy { it.time }
        for (i in 0 until sortedTides.lastIndex) {
            if (sortedTides[i].time <= time && sortedTides[i + 1].time >= time) {
                val period = Duration.between(sortedTides[i].time, sortedTides[i + 1].time)
                val constituent =
                    if (table.isSemidiurnal) TideConstituent.M2 else TideConstituent.K1
                val maxPeriod = Time.hours(180 / constituent.speed.toDouble() + 3.0)
                return !(sortedTides[i].isHigh == sortedTides[i + 1].isHigh || period > maxPeriod)
            }
        }
        return false
    }

    fun getCurrentTide(table: TideTable, time: ZonedDateTime = ZonedDateTime.now()): TideType? {
        val next = getNextTide(table, time)
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

    fun isRising(table: TideTable, time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return getNextTide(table, time).isHigh
    }

    private fun getNextTide(table: TideTable, time: ZonedDateTime): Tide {
        val todayTides = getTides(table, time.toLocalDate())
        val next = todayTides.firstOrNull { it.time >= time }
        return next ?: getNextTide(
            table,
            time.toLocalDate().plusDays(1).atStartOfDay().atZone(time.zone)
        )
    }
}