package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.optimization.GradientDescentOptimizer
import com.kylecorry.sol.science.astronomy.AstronomyService
import com.kylecorry.sol.science.astronomy.IAstronomyService
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ITimeProvider
import com.kylecorry.trail_sense.shared.sensors.SystemTimeProvider
import java.time.Duration
import java.time.ZonedDateTime

class SolarPanelService(
    private val astronomy: IAstronomyService = AstronomyService(),
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
        var time = start
        var total = 0.0
        val dtSeconds = dt.seconds

        while (time < end) {
            val radiation = astronomy.getSolarRadiation(time, location, tilt, bearing)
            if (radiation > 0) {
                total += dtSeconds / 3600.0 * radiation
            } else if (total != 0.0) {
                // The sun set
                break
            }
            time = time.plusSeconds(dtSeconds)
        }

        return total
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

        val startAzimuth = if (location.isNorthernHemisphere) {
            170.0
        } else {
            -10.0
        }

        val endAzimuth = if (location.isNorthernHemisphere) {
            300.0
        } else {
            120.0
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

        val optimizer = GradientDescentOptimizer(
            30.0,
            gradientFn = GradientDescentOptimizer.approximateGradientFn(0.1, fn = fn)
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