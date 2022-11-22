package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

internal class TemperatureCalculator(
    private val context: Context,
    private val location: Coordinate,
    private val date: LocalDate
) {

    private val astronomy = AstronomyService()
    private val maxTempOffset = Duration.ofHours(3)
    private val defaultMaxTime = date.atTime(14, 0).toZonedDateTime()
    private val today by lazy {
        astronomy.getSunTimes(
            location,
            SunTimesMode.Actual,
            date
        )
    }

    private val todayMin by lazy {
        today.rise ?: date.atStartOfDay().toZonedDateTime()
    }

    private val todayMax by lazy {
        today.transit?.plus(maxTempOffset) ?: defaultMaxTime
    }

    private val startTime by lazy {
        astronomy.getSunTimes(
            location,
            SunTimesMode.Actual,
            date.minusDays(1)
        ).transit?.plus(maxTempOffset) ?: defaultMaxTime.minusDays(1)
    }

    private val tomorrowMin by lazy {
        val tomorrow =
            astronomy.getSunTimes(location, SunTimesMode.Actual, date.plusDays(1))
        tomorrow.rise ?: date.atStartOfDay().minusDays(1).toZonedDateTime()
    }

    private val range by lazy {
        TemperatureEstimator(context).getDailyTemperatureRange(location, date)
    }

    private val wave1 by lazy {
        val firstVec = Vector2(0f, range.end.temperature)
        val secondVec = Vector2(getX(todayMin), range.start.temperature)
        Trigonometry.connect(firstVec, secondVec)
    }

    private val wave2 by lazy {
        val firstVec = Vector2(getX(todayMin), range.start.temperature)
        val secondVec = Vector2(getX(todayMax), range.end.temperature)
        Trigonometry.connect(firstVec, secondVec)
    }

    private val wave3 by lazy {
        val firstVec = Vector2(getX(todayMax), range.end.temperature)
        val secondVec = Vector2(getX(tomorrowMin), range.start.temperature)
        Trigonometry.connect(firstVec, secondVec)
    }

    private fun getX(time: ZonedDateTime): Float {
        return Time.hoursBetween(startTime, time)
    }

    fun getTemperature(time: ZonedDateTime): Temperature {
        val wave = if (time.isBefore(todayMin)) {
            wave1
        } else if (time.isBefore(todayMax)) {
            wave2
        } else {
            wave3
        }
        return Temperature.celsius(wave.calculate(getX(time)))
    }

}