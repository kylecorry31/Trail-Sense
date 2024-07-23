package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.cosDegrees
import com.kylecorry.sol.math.SolMath.sinDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.Trigonometry
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import java.time.Duration
import java.time.Instant

class FusedGPS(
    private val gps: ISatelliteGPS,
    private val interval: Duration,
    private val accelerometer: IAccelerometer? = null,
    private val useKalmanSpeed: Boolean = false,
    private val updateWithPrediction: Boolean = false
) : ISatelliteGPS, AbstractSensor() {
    override val altitude: Float
        get() = gps.altitude
    override val bearing: Bearing?
        get() = gps.bearing
    override val bearingAccuracy: Float?
        get() = gps.bearingAccuracy
    override val fixTimeElapsedNanos: Long?
        get() = gps.fixTimeElapsedNanos
    override val horizontalAccuracy: Float?
        get() = if (hasValidReading && currentAccuracy != 0f) currentAccuracy?.coerceAtLeast(
            KALMAN_MIN_ACCURACY
        ) else gps.horizontalAccuracy
    override val location: Coordinate
        get() = if (hasValidReading) currentLocation else gps.location
    override val mslAltitude: Float?
        get() = gps.mslAltitude
    override val rawBearing: Float?
        get() = gps.rawBearing
    override val satelliteDetails: List<Satellite>?
        get() = gps.satelliteDetails
    override val satellites: Int?
        get() = gps.satellites
    override val speed: Speed
        get() = if (hasValidReading && useKalmanSpeed) currentSpeed ?: gps.speed else gps.speed
    override val speedAccuracy: Float?
        get() = if (hasValidReading && useKalmanSpeed) currentSpeedAccuracy else gps.speedAccuracy
    override val time: Instant
        get() = gps.time
    override val verticalAccuracy: Float?
        get() = gps.verticalAccuracy
    override val hasValidReading: Boolean
        get() = currentLocation != Coordinate.zero && gps.hasValidReading
    override val quality: Quality
        get() = gps.quality

    private var kalman: FusedGPSFilter? = null
    private var currentLocation = Coordinate.zero
    private var currentAccuracy: Float? = null
    private var currentSpeed: Speed? = null
    private var currentSpeedAccuracy: Float? = null
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
        if (kalman == null || isFarFromReference()) {
            referenceLocation = gps.location
            referenceProjection = AzimuthalEquidistantProjection(
                referenceLocation,
                scale = PROJECTION_SCALE
            )
            val projectedLocation = getProjectedLocation()
            val projectedVelocity = getProjectedVelocity()
            kalman = FusedGPSFilter(
                true,
                projectedLocation.x,
                projectedLocation.y,
                projectedVelocity.x,
                projectedVelocity.y,
                ACCELERATION_DEVIATION * PROJECTION_SCALE,
                (gps.horizontalAccuracy ?: DEFAULT_POSITION_ACCURACY) * PROJECTION_SCALE,
                updateStateWithPrediction = updateWithPrediction
            )
        }

        val projectedLocation = getProjectedLocation()
        val projectedVelocity = getProjectedVelocity()
        kalman?.update(
            projectedLocation.x,
            projectedLocation.y,
            projectedVelocity.x,
            projectedVelocity.y,
            (gps.horizontalAccuracy ?: DEFAULT_POSITION_ACCURACY) * PROJECTION_SCALE,
            // If the device isn't moving, increase the speed accuracy
            (if (gps.speed.speed == 0f) NOT_MOVING_SPEED_ACCURACY_FACTOR else 1f) * (gps.speedAccuracy
                ?: DEFAULT_SPEED_ACCURACY) * PROJECTION_SCALE
        )

        updateCurrentFromKalman()
        notifyListeners()
        return true
    }

    private fun isFarFromReference(): Boolean {
        return gps.location.distanceTo(referenceLocation) > 200
    }

    private fun getProjectedVelocity(): Vector2 {
        val unitBearing = Trigonometry.toUnitAngle(gps.rawBearing ?: 0f, 90f, false)
        return Vector2(
            gps.speed.speed * cosDegrees(unitBearing) * PROJECTION_SCALE,
            gps.speed.speed * sinDegrees(unitBearing) * PROJECTION_SCALE
        )
    }

    private fun getProjectedLocation(): Vector2 {
        return referenceProjection.toPixels(gps.location)
    }

    private fun getKalmanLocation(): Coordinate {
        return referenceProjection.toCoordinate(
            Vector2(
                kalman?.currentX ?: 0f,
                kalman?.currentY ?: 0f
            )
        )
    }

    private fun getKalmanLocationAccuracy(): Float {
        return (kalman?.currentPositionError?.div(PROJECTION_SCALE) ?: 0f)
    }

    private fun getKalmanSpeed(): Speed {
        val velocity = Vector2(
            kalman?.currentXVelocity ?: 0f,
            kalman?.currentYVelocity ?: 0f
        )
        return Speed(
            velocity.magnitude() / PROJECTION_SCALE,
            DistanceUnits.Meters,
            TimeUnits.Seconds
        )
    }

    private fun getKalmanSpeedAccuracy(): Float {
        return (kalman?.currentVelocityError?.div(PROJECTION_SCALE) ?: 0f)
    }

    private fun updateCurrentFromKalman() {
        val newLocation = getKalmanLocation()
        currentLocation = Coordinate(
            newLocation.latitude.coerceIn(-90.0, 90.0),
            SolMath.wrap(newLocation.longitude, -180.0, 180.0)
        )
        currentAccuracy = getKalmanLocationAccuracy()
        currentSpeed = getKalmanSpeed()
        currentSpeedAccuracy = getKalmanSpeedAccuracy()
    }

    private fun update() {
        if (!gps.hasValidReading || currentLocation == Coordinate.zero || kalman == null) return

        kalman?.predict(
            (accelerometer?.rawAcceleration?.get(0) ?: 0f) * PROJECTION_SCALE,
            (accelerometer?.rawAcceleration?.get(1) ?: 0f) * PROJECTION_SCALE,
        )
        lastPredictTime = Instant.now()

        updateCurrentFromKalman()
        if (updateWithPrediction) {
            notifyListeners()
        }
    }

    companion object {
        private const val PROJECTION_SCALE = 1.0f
        private const val NOT_MOVING_SPEED_ACCURACY_FACTOR = 0.5f
        private const val DEFAULT_SPEED_ACCURACY = 0.05f
        private const val DEFAULT_POSITION_ACCURACY = 30.0f

        // Process noise
        private const val ACCELERATION_DEVIATION = 0.1f
        private const val KALMAN_MIN_ACCURACY = 4f
    }

}