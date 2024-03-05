package com.kylecorry.trail_sense.shared.sensors.gps

import android.util.Log
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.math.filters.ComplementaryFilter
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import java.time.Duration
import java.time.Instant
import kotlin.math.max

class FusedGPS2(
    private val gps: IGPS,
    private val interval: Duration,
    private val verbose: Boolean = false
) : IGPS, AbstractSensor() {
    override val altitude: Float
        get() = gps.altitude
    override val bearing: Bearing?
        get() = gps.bearing
    override val bearingAccuracy: Float?
        get() = gps.bearingAccuracy
    override val horizontalAccuracy: Float?
        get() = gps.horizontalAccuracy
    override val location: Coordinate
        get() = if (hasValidReading) currentLocation else gps.location
    override val mslAltitude: Float?
        get() = gps.mslAltitude
    override val rawBearing: Float?
        get() = gps.rawBearing
    override val satellites: Int?
        get() = gps.satellites
    override val speed: Speed
        get() = gps.speed
    override val speedAccuracy: Float?
        get() = gps.speedAccuracy
    override val time: Instant
        get() = gps.time.plus(Duration.between(gpsReadingSystemTime, Instant.now()))
    override val verticalAccuracy: Float?
        get() = gps.verticalAccuracy
    override val hasValidReading: Boolean
        get() = currentLocation != Coordinate.zero && gps.hasValidReading
    override val quality: Quality
        get() = gps.quality

    private var currentLocation = Coordinate.zero
    private var referenceLocation = Coordinate.zero
    private var referenceProjection =
        AzimuthalEquidistantProjection(referenceLocation, scale = PROJECTION_SCALE.toFloat())

    private var gpsReadingTime = Instant.now()
    private var gpsReadingSystemTime = Instant.now()
    private var lastPredictTime = Instant.now()

    private val timer = CoroutineTimer {
        update()
    }

    override fun startImpl() {
        gps.start(this::onGPSUpdate)
        timer.interval(interval)
    }

    override fun stopImpl() {
        gps.stop(this::onGPSUpdate)
        timer.stop()
    }

    private fun onGPSUpdate(): Boolean {
        gpsReadingTime = gps.time
        gpsReadingSystemTime = Instant.now()
        lastPredictTime = Instant.now()
        if (currentLocation == Coordinate.zero || isFarFromReference(gps.location)) {
            referenceLocation = gps.location
            referenceProjection = AzimuthalEquidistantProjection(
                referenceLocation,
                scale = PROJECTION_SCALE.toFloat()
            )
            currentLocation = referenceLocation
            notifyListeners()
        }
        return true
    }

    private fun isFarFromReference(location: Coordinate): Boolean {
        return location.distanceTo(referenceLocation) > 200
    }

    private fun getProjectedLocation(location: Coordinate = gps.location): Vector2 {
        return referenceProjection.toPixels(location)
    }

    private val xFilter = ComplementaryFilter(listOf(1f, 1f, 1f))
    private val yFilter = ComplementaryFilter(listOf(1f, 1f, 1f))

    private fun update() {
        if (!gps.hasValidReading || currentLocation == Coordinate.zero) return

        val dt = Duration.between(lastPredictTime, Instant.now()).toMillis() / 1000.0
        lastPredictTime = Instant.now()

        val timeSinceLastMeasurement = Duration.between(gpsReadingSystemTime, Instant.now())
        val estimateAlpha = getEstimateAlpha(timeSinceLastMeasurement)
        val gpsAlpha = getGPSAlpha(gps.horizontalAccuracy ?: 30f)
        val speedAlpha = getSpeedAlpha(gps.speedAccuracy ?: 10f, timeSinceLastMeasurement)
        if (verbose) {
            Log.d(
                "FusedGPS",
                "Estimate alpha: ${
                    DecimalFormatter.format(
                        estimateAlpha,
                        2,
                        true
                    )
                }, GPS alpha: ${
                    DecimalFormatter.format(
                        gpsAlpha,
                        2,
                        true
                    )
                }, Speed alpha: ${DecimalFormatter.format(speedAlpha, 2, true)}"
            )
        }
        xFilter.weights = listOf(estimateAlpha, gpsAlpha, speedAlpha)
        yFilter.weights = listOf(estimateAlpha, gpsAlpha, speedAlpha)

        // TODO: Filter for speed

        val estimate = getProjectedLocation(currentLocation)
        val gpsLocation = getProjectedLocation(gps.location)
        val projectedLocation = getProjectedLocation(
            currentLocation.plus(
                gps.speed.speed * dt * PROJECTION_SCALE,
                Bearing(gps.rawBearing ?: 0f)
            )
        )

        // Combine the estimates
        val xEstimate = xFilter.filter(
            listOf(
                estimate.x,
                gpsLocation.x,
                projectedLocation.x
            )
        )

        val yEstimate = yFilter.filter(
            listOf(
                estimate.y,
                gpsLocation.y,
                projectedLocation.y
            )
        )

        // Convert to coordinate
        val coord = referenceProjection.toCoordinate(Vector2(xEstimate, yEstimate))
        if (coord.latitude in -90.0..90.0 && coord.longitude in -180.0..180.0) {
            currentLocation = coord
        }
        // TODO: Update accuracy
        // TODO: Update speed

        notifyListeners()
    }

    private fun getEstimateAlpha(timeSinceMeasurement: Duration): Float {
        val minAlphaInverse = 0.4f
        val minTime = 0f
        val maxAlphaInverse = 0.9f
        val maxTime = 10f
        val time = timeSinceMeasurement.toMillis() / 1000f
        return 1 - SolMath.map(
            time,
            minTime,
            maxTime,
            minAlphaInverse,
            maxAlphaInverse,
            shouldClamp = true
        )
    }

    private fun getGPSAlpha(accuracy: Float): Float {
        val minAlphaInverse = 0.4f
        val minAccuracy = 0f
        val maxAlphaInverse = 0.9f
        val maxAccuracy = 30f
        return 1 - SolMath.map(
            accuracy,
            minAccuracy,
            maxAccuracy,
            minAlphaInverse,
            maxAlphaInverse,
            shouldClamp = true
        )
    }

    private fun getSpeedAlpha(accuracy: Float, timeSinceMeasurement: Duration): Float {
        val minSpeedAlphaInverse = 0.6f
        val minAccuracy = 0f
        val maxSpeedAlphaInverse = 0.95f
        val maxAccuracy = 30f
        val accuracyAlpha = SolMath.map(
            accuracy,
            minAccuracy,
            maxAccuracy,
            minSpeedAlphaInverse,
            maxSpeedAlphaInverse,
            shouldClamp = true
        )

        val minTimeAlphaInverse = 0.6f
        val minTime = 0f
        val maxTimeAlphaInverse = 0.95f
        val maxTime = 10f
        val timeAlpha = SolMath.map(
            timeSinceMeasurement.toMillis() / 1000f,
            minTime,
            maxTime,
            minTimeAlphaInverse,
            maxTimeAlphaInverse,
            shouldClamp = true
        )

        return (accuracyAlpha + timeAlpha) / 2
    }

    companion object {
        private const val PROJECTION_SCALE = 1.0
    }

}