package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideFrequency
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

class TideService {

    private val ocean = OceanographyService()

    fun getTides(tide: TideEntity, date: LocalDate): List<Tide> {
        val harmonics = ocean.estimateHarmonics(
            tide.reference,
            tide.frequency
        )
        return ocean.getTides(harmonics, date.atStartOfDay().toZonedDateTime())
    }

    fun getTides(table: TideTable, date: LocalDate): List<Tide> {
        var time = date.atStartOfDay().toZonedDateTime()
        val tides = mutableListOf<Tide>()
        var previous = getWaterLevel(table, time.minusMinutes(1))
        var next = getWaterLevel(table, time)
        while (time.toLocalDate() == date){
            val level = next
            next = getWaterLevel(table, time.plusMinutes(1))
            val isHigh = previous < level && next < level
            val isLow = previous > level && next > level

            if (isHigh){
                tides.add(Tide.high(time, level))
            }

            if (isLow){
                tides.add(Tide.low(time, level))
            }

            previous = level
            time = time.plusMinutes(1)
        }

        return tides
    }

    fun getWaterLevel(table: TideTable, time: ZonedDateTime): Float {
        if (table.tides.size == 1){
            val tide = table.tides[0]
            val harmonics = ocean.estimateHarmonics(
                tide.time,
                TideFrequency.Semidiurnal
            )
            return ocean.getWaterLevel(harmonics, time)
        }

        val sortedTides = table.tides.sortedBy { it.time }
        val idx = sortedTides.indexOfFirst { it.time >= time }

        return if (idx == -1 || idx == sortedTides.lastIndex){
            // TODO: estimate the tide
            0f
        } else {
            // TODO: Make sure these two tides aren't too far apart
            val t = Duration.between(sortedTides[idx].time, time).seconds / 3600f
            getSineWaveBetween(sortedTides[idx], sortedTides[idx + 1]).cosine(t)
        }
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

    fun getWaterLevels(tide: TideEntity, date: LocalDate): List<Reading<Float>> {
        val harmonics = ocean.estimateHarmonics(
            tide.reference,
            tide.frequency
        )
        val granularityMinutes = 10L
        var time = date.atStartOfDay().toZonedDateTime()

        val levels = mutableListOf<Reading<Float>>()
        while (time.toLocalDate() == date) {
            levels.add(
                Reading(
                    ocean.getWaterLevel(harmonics, time),
                    time.toInstant()
                )
            )
            time = time.plusMinutes(granularityMinutes)
        }

        return levels
    }

    fun getCurrentTide(tide: TideEntity, time: LocalDateTime = LocalDateTime.now()): TideType? {
        val next = getNextTide(tide, time)
        val timeToNextTide = Duration.between(time, next.time)
        return if (next.type == TideType.High && timeToNextTide < Duration.ofHours(2) || (next.type == TideType.Low && timeToNextTide > Duration.ofHours(
                4
            ))
        ) {
            TideType.High
        } else if (next.type == TideType.Low && timeToNextTide < Duration.ofHours(2) || (next.type == TideType.High && timeToNextTide > Duration.ofHours(
                4
            ))
        ) {
            TideType.Low
        } else {
            null
        }
    }

    fun getCurrentTide(table: TideTable, time: LocalDateTime = LocalDateTime.now()): TideType? {
        val next = getNextTide(table, time)
        val timeToNextTide = Duration.between(time, next.time)
        return if (next.type == TideType.High && timeToNextTide < Duration.ofHours(2) || (next.type == TideType.Low && timeToNextTide > Duration.ofHours(
                4
            ))
        ) {
            TideType.High
        } else if (next.type == TideType.Low && timeToNextTide < Duration.ofHours(2) || (next.type == TideType.High && timeToNextTide > Duration.ofHours(
                4
            ))
        ) {
            TideType.Low
        } else {
            null
        }
    }

    fun isRising(tide: TideEntity, time: LocalDateTime = LocalDateTime.now()): Boolean {
        return getNextTide(tide, time).type == TideType.High
    }

    fun isRising(table: TideTable, time: LocalDateTime = LocalDateTime.now()): Boolean {
        return getNextTide(table, time).type == TideType.High
    }

    private fun getNextTide(tide: TideEntity, time: LocalDateTime): Tide {
        val todayTides = getTides(tide, time.toLocalDate())
        val next = todayTides.firstOrNull { it.time > time.toZonedDateTime() }
        return next ?: getNextTide(tide, time.toLocalDate().atStartOfDay().plusDays(1))
    }

    private fun getNextTide(table: TideTable, time: LocalDateTime): Tide {
        val todayTides = getTides(table, time.toLocalDate())
        val next = todayTides.firstOrNull { it.time > time.toZonedDateTime() }
        return next ?: getNextTide(table, time.toLocalDate().atStartOfDay().plusDays(1))
    }

    private fun getSineWaveBetween(tide1: Tide, tide2: Tide): SineWave {
        val period = Duration.between(tide1.time, tide2.time).seconds / 3600f
        val deltaHeight = abs(tide1.height - tide2.height)
        val verticalShift = deltaHeight / 2 + min(tide1.height, tide2.height)
        val frequency = (360 / (2 * period)).toRadians()
        val amplitude = (if (tide1.type == TideType.High) 1 else -1) * deltaHeight / 2
        return SineWave(amplitude, frequency, 0f, verticalShift)
    }


    private data class SineWave(
        val amplitude: Float,
        val frequency: Float,
        val horizontalShift: Float,
        val verticalShift: Float
    ){
        fun cosine(x: Float): Float {
            return amplitude * cos(frequency * (x - horizontalShift)) + verticalShift
        }
    }

}