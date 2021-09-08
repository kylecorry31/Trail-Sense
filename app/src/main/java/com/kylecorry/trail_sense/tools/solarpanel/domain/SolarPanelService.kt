package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
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
import kotlin.math.pow

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


    private fun getSolarRadiationGradientForRemainderOfDay(
        start: ZonedDateTime,
        end: ZonedDateTime,
        location: Coordinate,
        tilt: Float,
        bearing: Bearing,
        dt: Duration = Duration.ofMinutes(15)
    ): Pair<Double, Double> {
        var time = start
        var totalX = 0.0
        var totalY = 0.0
        val dtSeconds = dt.seconds

        while (time < end) {
            val radiation = getRadiationPanelGradient(time, location, tilt, bearing)
            totalX += dtSeconds / 3600.0 * radiation.first
            totalY += dtSeconds / 3600.0 * radiation.second
            time = time.plusSeconds(dtSeconds)
        }

        return totalX to totalY
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
            70.0
        } else {
            -120.0
        }

        val endAzimuth = if (location.isNorthernHemisphere) {
            300.0
        } else {
            10.0
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
            30.0
        ) { x: Double, y: Double ->
            getSolarRadiationGradientForRemainderOfDay(
                start,
                end,
                location,
                y.toFloat(),
                Bearing(x.toFloat()),
                energyResolution
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

    /**
     * Gets the solar radiation in kW/m^2 at the given time and location
     */
    fun getRadiationPanelGradient(
        time: ZonedDateTime,
        location: Coordinate,
        tilt: Float,
        bearing: Bearing
    ): Pair<Double, Double> {
        val altitude = astronomy.getSunAltitude(time, location).toDouble()
        if (altitude < 0){
            return 0.0 to 0.0
        }
        val am = 1 / SolMath.cosDegrees(90 - altitude)
        val radiantPowerDensity =
            (1 + 0.033 * SolMath.cosDegrees(360 * (time.dayOfYear - 2) / 365.0)) * 1.353
        val incident = radiantPowerDensity * 0.7.pow(am.pow(0.678))

        val azimuth = astronomy.getSunAzimuth(time, location).value.toDouble()

        val gradientAzimuth = incident * (
                -SolMath.cosDegrees(altitude) * SolMath.sinDegrees(tilt) * SolMath.sinDegrees(
                    bearing.value - azimuth
                ) + SolMath.sinDegrees(
                    altitude
                ) * SolMath.cosDegrees(tilt)
                )

        val gradientTilt = incident * (
                SolMath.cosDegrees(altitude) * SolMath.cosDegrees(tilt) * SolMath.cosDegrees(bearing.value - azimuth) - SolMath.sinDegrees(
                    altitude
                ) * SolMath.sinDegrees(tilt)
                )

        return gradientAzimuth to gradientTilt
    }

}