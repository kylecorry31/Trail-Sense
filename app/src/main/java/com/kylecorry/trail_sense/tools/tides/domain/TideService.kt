package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.andromeda.core.rangeOrNull
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideTableWaterLevelCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TideService {

    fun getTides(table: TideTable, date: LocalDate): List<Tide> {
        // TODO: Calculate the tides instead of brute forcing if possible
        var time = date.atStartOfDay().toZonedDateTime()
        val tides = mutableListOf<Tide>()
        var previous = getWaterLevel(table, time.minusMinutes(1))
        var next = getWaterLevel(table, time)
        while (time.toLocalDate() == date) {
            val level = next
            next = getWaterLevel(table, time.plusMinutes(1))
            val isHigh = previous < level && next < level
            val isLow = previous > level && next > level

            if (isHigh) {
                tides.add(Tide.high(time, level))
            }

            if (isLow) {
                tides.add(Tide.low(time, level))
            }

            previous = level
            time = time.plusMinutes(1)
        }

        return tides
    }

    fun getWaterLevel(table: TideTable, time: ZonedDateTime): Float {
        val strategy = TideTableWaterLevelCalculator(table)
        return strategy.calculate(time)
    }

    fun getWaterLevels(table: TideTable, date: LocalDate): List<Reading<Float>> {
        val granularityMinutes = 10L
        var time = date.atStartOfDay().toZonedDateTime()

        val levels = mutableListOf<Reading<Float>>()
        while (time.toLocalDate() == date) {
            levels.add(
                Reading(
                    getWaterLevel(table, time),
                    time.toInstant()
                )
            )
            time = time.plusMinutes(granularityMinutes)
        }

        return levels
    }

    fun getRange(table: TideTable): Range<Float> {
        return if (table.tides.size <= 1) {
            Range(-1f, 1f)
        } else {
            val range = table.tides.mapNotNull { it.height }.rangeOrNull()
            Range(range?.lower ?: -1f, range?.upper ?: 1f)
        }
    }

    fun isWithinTideTable(table: TideTable, time: LocalDateTime = LocalDateTime.now()): Boolean {
        val sortedTides = table.tides.sortedBy { it.time }
        for (i in 0 until sortedTides.lastIndex) {
            if (sortedTides[i].time <= time.toZonedDateTime() && sortedTides[i + 1].time >= time.toZonedDateTime()) {
                val period = Duration.between(sortedTides[i].time, sortedTides[i + 1].time)
                val constituent =
                    if (table.frequency == TideFrequency.Semidiurnal) TideConstituent.M2 else TideConstituent.K1
                val maxPeriod = Time.hours(180 / constituent.speed.toDouble() + 3.0)
                return !(sortedTides[i].isHigh == sortedTides[i + 1].isHigh || period > maxPeriod)
            }
        }
        return false
    }

    fun getCurrentTide(table: TideTable, time: LocalDateTime = LocalDateTime.now()): TideType? {
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

    fun isRising(table: TideTable, time: LocalDateTime = LocalDateTime.now()): Boolean {
        return getNextTide(table, time).isHigh
    }

    private fun getNextTide(table: TideTable, time: LocalDateTime): Tide {
        val todayTides = getTides(table, time.toLocalDate())
        val next = todayTides.firstOrNull { it.time >= time.toZonedDateTime() }
        return next ?: getNextTide(table, time.toLocalDate().atStartOfDay().plusDays(1))
    }
}