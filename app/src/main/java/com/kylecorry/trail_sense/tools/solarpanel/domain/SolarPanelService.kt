package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.calculus.Calculus
import com.kylecorry.sol.math.optimization.HillClimbingOptimizer
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.plusHours
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ITimeProvider
import com.kylecorry.trail_sense.shared.sensors.SystemTimeProvider
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

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
        duration: Duration = Duration.ofDays(1)
    ): Float {
        val time = timeProvider.getTime()
        var end = time.plus(duration)
        if (end.toLocalDate() != time.toLocalDate()) {
            end = time.atEndOfDay()
        }
        return getSolarRadiationForRemainderOfDay(
            time,
            end,
            location,
            tilt,
            azimuth
        ).toFloat()
    }

    fun getBestPosition(
        location: Coordinate,
        maxDuration: Duration
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
            )
        )
    }

    private fun getSolarRadiationForRemainderOfDay(
        start: ZonedDateTime,
        end: ZonedDateTime,
        location: Coordinate,
        tilt: Float,
        bearing: Bearing,
        dt: Duration = Duration.ofMinutes(15)
    ): Double {
        val secondsToHours = 1.0 / (60 * 60)
        return Calculus.integral(
            0.0,
            Duration.between(start, end).seconds * secondsToHours,
            dt.seconds * secondsToHours
        ) { hours ->
            val t = start.toLocalDateTime().plusHours(hours).toZonedDateTime()
            Astronomy.getSolarRadiation(t, location, tilt, bearing)
        }
    }

    private fun getBestPosition(
        location: Coordinate,
        start: ZonedDateTime = timeProvider.getTime(),
        maxDuration: Duration = Duration.ofDays(1),
        energyResolution: Duration = Duration.ofMinutes(30)
    ): Pair<Float, Bearing> {
        var end = start.plus(maxDuration)
        if (end.toLocalDate() != start.toLocalDate()) {
            end = start.atEndOfDay()
        }

        val sunAzimuth = Astronomy.getSunAzimuth(start, location).value.toDouble()

        val startAzimuth = if (location.isNorthernHemisphere) {
            // East
            max(80.0, sunAzimuth)
        } else {
            // West
            -100.0
        }

        val endAzimuth = if (location.isNorthernHemisphere) {
            // West
            280.0
        } else {
            // East
            val sunBearing = if (sunAzimuth < 180) {
                sunAzimuth
            } else {
                sunAzimuth - 360
            }
            min(100.0, sunBearing)
        }

        val startTilt = 0.0
        val endTilt = 90.0

        val fn = { x: Double, y: Double ->
            getSolarRadiationForRemainderOfDay(
                start,
                end,
                location,
                y.toFloat(),
                Bearing(x.toFloat()),
                energyResolution
            )
        }

        val optimizer = HillClimbingOptimizer(
            1.0,
            2000
        )

        val best = optimizer.optimize(
            Range(startAzimuth, endAzimuth),
            Range(startTilt, endTilt),
            true,
            fn = fn
        )

        return Pair(best.second.toFloat(), Bearing(best.first.toFloat()))
    }
}