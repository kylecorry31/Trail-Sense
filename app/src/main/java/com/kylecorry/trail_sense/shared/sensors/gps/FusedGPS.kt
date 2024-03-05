package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import java.time.Duration
import java.time.Instant

class FusedGPS(
    private val gps: IGPS,
    private val interval: Duration,
    private val accelerometer: IAccelerometer? = null,
) : IGPS, AbstractSensor() {
    override val altitude: Float
        get() = gps.altitude
    override val bearing: Bearing?
        get() = gps.bearing
    override val bearingAccuracy: Float?
        get() = gps.bearingAccuracy
    override val horizontalAccuracy: Float?
        get() = if (hasValidReading) currentAccuracy else gps.horizontalAccuracy
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
        get() = gps.time.plus(Duration.between(gpsReadingSystemTime, lastPredictTime))
    override val verticalAccuracy: Float?
        get() = gps.verticalAccuracy
    override val hasValidReading: Boolean
        get() = currentLocation != Coordinate.zero && gps.hasValidReading
    override val quality: Quality
        get() = gps.quality

    private var kalman: FusedGPSFilter? = null
    private var currentLocation = Coordinate.zero
    private var currentAccuracy: Float? = null
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
        kalman = null
        gps.start(this::onGPSUpdate)
        accelerometer?.start(this::onAccelerometerUpdate)
        timer.interval(interval)
    }

    override fun stopImpl() {
        gps.stop(this::onGPSUpdate)
        accelerometer?.stop(this::onAccelerometerUpdate)
        timer.stop()
    }

    private fun onAccelerometerUpdate(): Boolean {
        return true
    }

    private fun onGPSUpdate(): Boolean {
        gpsReadingTime = gps.time
        gpsReadingSystemTime = Instant.now()
        lastPredictTime = Instant.now()
        if (kalman == null || isFarFromReference(gps.location)) {
            referenceLocation = gps.location
            referenceProjection = AzimuthalEquidistantProjection(
                referenceLocation,
                scale = PROJECTION_SCALE.toFloat()
            )
            val projectedLocation = getProjectedLocation()
            val projectedVelocity = getProjectedVelocity()
            kalman = FusedGPSFilter(
                true,
                projectedLocation.x.toDouble(),
                projectedLocation.y.toDouble(),
                projectedVelocity.x.toDouble(),
                projectedVelocity.y.toDouble(),
                0.1 * PROJECTION_SCALE,
                (gps.horizontalAccuracy?.toDouble() ?: 30.0) * PROJECTION_SCALE
            )
        }

        val projectedLocation = getProjectedLocation()
        val projectedVelocity = getProjectedVelocity()
        kalman?.update(
            projectedLocation.x.toDouble(),
            projectedLocation.y.toDouble(),
            projectedVelocity.x.toDouble(),
            projectedVelocity.y.toDouble(),
            (gps.horizontalAccuracy?.toDouble() ?: 30.0) * PROJECTION_SCALE,
            (gps.speedAccuracy?.toDouble() ?: 1.0) * PROJECTION_SCALE
        )

        currentLocation = getKalmanLocation()
        currentAccuracy = (kalman?.positionError?.div(PROJECTION_SCALE)?.toFloat() ?: 0f)
        notifyListeners()
        return true
    }

    private fun isFarFromReference(location: Coordinate): Boolean {
        return location.distanceTo(referenceLocation) > 200
    }

    private fun getProjectedVelocity(): Vector2 {
        val unitBearing = Trigonometry.toUnitAngle(gps.rawBearing ?: 0f, 90f, false)
        return Vector2(
            gps.speed.speed * cosDegrees(unitBearing) * PROJECTION_SCALE.toFloat(),
            gps.speed.speed * sinDegrees(unitBearing) * PROJECTION_SCALE.toFloat()
        )
    }

    private fun getProjectedLocation(): Vector2 {
        return referenceProjection.toPixels(gps.location)
    }

    private fun getKalmanLocation(): Coordinate {
        return referenceProjection.toCoordinate(
            Vector2(
                kalman?.currentX?.toFloat() ?: 0f,
                kalman?.currentY?.toFloat() ?: 0f
            )
        )
    }

    private fun update() {
        if (!gps.hasValidReading || currentLocation == Coordinate.zero || kalman == null) return

        kalman?.predict(
            (accelerometer?.rawAcceleration?.get(0)?.toDouble() ?: 0.0) * PROJECTION_SCALE,
            (accelerometer?.rawAcceleration?.get(1)?.toDouble() ?: 0.0) * PROJECTION_SCALE,
        )
        lastPredictTime = Instant.now()

        val newLocation = getKalmanLocation()
        if (newLocation.latitude in -90.0..90.0 && newLocation.longitude in -180.0..180.0) {
            currentLocation = newLocation
            currentAccuracy = (kalman?.positionError?.div(PROJECTION_SCALE)?.toFloat() ?: 0f)
        }
        notifyListeners()
    }

    companion object {
        private const val PROJECTION_SCALE = 1.0
    }

}