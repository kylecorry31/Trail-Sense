package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.calculus.Calculus
import com.kylecorry.sol.math.optimization.HillClimbingOptimizer
import com.kylecorry.sol.math.optimization.SimulatedAnnealingOptimizer
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import java.time.Duration
import java.time.ZonedDateTime

class SolarPanelService(
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    /**
     * Gets the solar radiation in kWh / m^2
     */
    fun getSolarEnergy(
        location: Coordinate,
        tilt: Float,
        azimuth: Bearing,
        duration: Duration = Duration.ofDays(1),
        restrictToToday: Boolean = false
    ): Float {
        val time = timeProvider.getTime()
        var end = time.plus(duration)
        if (end.toLocalDate() != time.toLocalDate() && restrictToToday) {
            end = time.atEndOfDay()
        }
        return getSolarRadiation(
            time,
            end,
            location,
            tilt,
            azimuth
        ).toFloat().coerceAtLeast(0f)
    }

    fun getBestPosition(
        location: Coordinate,
        maxDuration: Duration,
        restrictToToday: Boolean = false
    ): Pair<Float, Bearing> {
        val duration = if (maxDuration <= Duration.ofMinutes(15).plusSeconds(5)) {
            Duration.ofMinutes(15).plusSeconds(15)
        } else {
            maxDuration
        }

        return getBestPosition(
            location,
            maxDuration = duration,
            energyResolution = if (duration < Duration.ofHours(6)) Duration.ofMinutes(15) else Duration.ofMinutes(
                30
            ),
            restrictToToday = restrictToToday
        )
    }

    private fun getSolarRadiation(
        start: ZonedDateTime,
        end: ZonedDateTime,
        location: Coordinate,
        tilt: Float,
        bearing: Bearing,
        dt: Duration = Duration.ofMinutes(15),
    ): Double {
        val secondsToHours = 1.0 / (60 * 60)
        val original = Calculus.integral(
            0.0,
            Duration.between(start, end).seconds * secondsToHours,
            dt.seconds * secondsToHours
        ) { hours ->
            val t = start.plus(Time.hours(hours))
            Astronomy.getSolarRadiation(t, location, tilt, bearing, withRefraction = true)
                .coerceAtLeast(0.0)
        }

        if (original > 0) {
            return original
        }

        // Try again, but allowing negative values
        return Calculus.integral(
            0.0,
            Duration.between(start, end).seconds * secondsToHours,
            dt.seconds * secondsToHours
        ) { hours ->
            val t = start.plus(Time.hours(hours))
            Astronomy.getSolarRadiation(t, location, tilt, bearing, withRefraction = true)
        }
    }

    private fun getBestPosition(
        location: Coordinate,
        start: ZonedDateTime = timeProvider.getTime(),
        maxDuration: Duration = Duration.ofDays(1),
        energyResolution: Duration = Duration.ofMinutes(30),
        restrictToToday: Boolean = false
    ): Pair<Float, Bearing> {
        var end = start.plus(maxDuration)
        if (end.toLocalDate() != start.toLocalDate() && restrictToToday) {
            end = start.atEndOfDay()
        }

        val startAzimuth = if (location.isNorthernHemisphere) {
            // East
            80.0
        } else {
            // West
            -100.0
        }

        val endAzimuth = if (location.isNorthernHemisphere) {
            // West
            280.0
        } else {
            // East
            100.0
        }

        val startTilt = 0.0
        val endTilt = 90.0

        val fn = { x: Double, y: Double ->
            getSolarRadiation(
                start,
                end,
                location,
                y.toFloat(),
                Bearing(x.toFloat()),
                energyResolution
            )
        }

        val optimizer = if (start.toLocalDate() == end.toLocalDate()) {
            HillClimbingOptimizer(
                1.0,
                2000
            )
        } else {
            // When crossing days, hill climbing can get stuck in a local maximum
            SimulatedAnnealingOptimizer(
                10.0,
                1.0,
                2000
            )
        }

        val best = optimizer.optimize(
            Range(startAzimuth, endAzimuth),
            Range(startTilt, endTilt),
            true,
            fn = fn
        )

        return Pair(best.second.toFloat(), Bearing(best.first.toFloat()))
    }
}