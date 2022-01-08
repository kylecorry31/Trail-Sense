package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.OceanographyService
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class TideService {

    private val ocean = OceanographyService()

    fun getTides(tide: TideEntity, date: LocalDate): List<Tide> {
        val harmonics = ocean.estimateHarmonics(
            tide.reference,
            tide.frequency
        )
        return ocean.getTides(harmonics, date.atStartOfDay().toZonedDateTime())
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

    fun getCurrentTide(tide: TideEntity, time: LocalDateTime = LocalDateTime.now()): TideType {
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
            TideType.Half
        }
    }

    private fun getNextTide(tide: TideEntity, time: LocalDateTime): Tide {
        val todayTides = getTides(tide, time.toLocalDate())
        val next = todayTides.firstOrNull { it.time > time.toZonedDateTime() }
        return next ?: getNextTide(tide, time.toLocalDate().atStartOfDay().plusDays(1))
    }

}