package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.science.astronomy.AstronomyService
import com.kylecorry.sol.science.astronomy.IAstronomyService
import com.kylecorry.sol.science.astronomy.SolarPanelPosition
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
    fun getSolarEnergy(location: Coordinate, position: SolarPanelPosition): Float {
        val time = timeProvider.getTime()
        return getSolarRadiationForRemainderOfDay(
            time,
            time.atEndOfDay(),
            location,
            position
        )
    }

    fun getBestPosition(
        location: Coordinate,
        maxDuration: Duration
    ): SolarPanelPosition {

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
        position: SolarPanelPosition,
        dt: Duration = Duration.ofMinutes(15)
    ): Float {
        var time = start
        var total = 0.0
        val dtSeconds = dt.seconds

        while (time < end) {
            val radiation = astronomy.getSolarRadiation(time, location, position)
            if (radiation > 0) {
                total += dtSeconds / 3600.0 * radiation
            } else if (total != 0.0) {
                // The sun set
                break
            }
            time = time.plusSeconds(dtSeconds)
        }

        return total.toFloat()
    }

    private fun getBestPosition(
        location: Coordinate,
        start: ZonedDateTime = timeProvider.getTime(),
        maxDuration: Duration = Duration.ofDays(1),
        energyResolution: Duration = Duration.ofMinutes(30)
    ): SolarPanelPosition {
        // TODO: Replace with a faster optimization algorithm (maybe simulated annealing)
        var end = start.plus(maxDuration)
        if (end.toLocalDate() != start.toLocalDate()) {
            end = start.atEndOfDay()
        }

        val startAzimuth = if (location.isNorthernHemisphere) {
            170
        } else {
            -10
        }

        val endAzimuth = if (location.isNorthernHemisphere) {
            280
        } else {
            100
        }

        // TODO: Set start azimuth to min of current sun azimuth and start azimuth

        var startTilt = 0
        val endTilt = 90

        var maxAzimuth = startAzimuth
        var maxTilt = startTilt
        var maxRadiation = 0f

        var lastAzimuthRadiation = 0f
        for (azimuth in startAzimuth..endAzimuth) {
            var lastTiltRadiation = 0f
            for (tilt in startTilt..endTilt) {
                val radiation = getSolarRadiationForRemainderOfDay(
                    start,
                    end,
                    location,
                    SolarPanelPosition(tilt.toFloat(), Bearing(azimuth.toFloat())),
                    energyResolution
                )

                if (radiation < lastTiltRadiation) {
                    startTilt = (tilt - 20).coerceAtLeast(0)
                    break
                }

                lastTiltRadiation = radiation

                if (radiation > maxRadiation) {
                    maxAzimuth = azimuth
                    maxTilt = tilt
                    maxRadiation = radiation
                }
            }
            if (lastTiltRadiation < lastAzimuthRadiation) {
                break
            }

            lastAzimuthRadiation = lastTiltRadiation
        }

        return SolarPanelPosition(maxTilt.toFloat(), Bearing(maxAzimuth.toFloat()))
    }

}