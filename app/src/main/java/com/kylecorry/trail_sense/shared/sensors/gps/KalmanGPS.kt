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

class KalmanGPS(
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
        get() = if (hasValidReading) kalman?.positionError?.toFloat() else gps.horizontalAccuracy
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
        get() = gps.time
    override val verticalAccuracy: Float?
        get() = gps.verticalAccuracy
    override val hasValidReading: Boolean
        get() = currentLocation != Coordinate.zero && gps.hasValidReading
    override val quality: Quality
        get() = gps.quality

    private var kalman: FusedGPSFilter? = null
    private var currentLocation = Coordinate.zero
    private var referenceLocation = Coordinate.zero
    private var referenceProjection = AzimuthalEquidistantProjection(referenceLocation)

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

        if (kalman == null || isFarFromReference(gps.location)) {
            referenceLocation = gps.location
            referenceProjection = AzimuthalEquidistantProjection(referenceLocation)
            kalman = FusedGPSFilter(
                true,
                getXPosition(gps.location).toDouble(),
                getYPosition(gps.location).toDouble(),
                getXVelocity(gps.speed, gps.rawBearing ?: 0f).toDouble(),
                getYVelocity(gps.speed, gps.rawBearing ?: 0f).toDouble(),
                0.1,
                gps.horizontalAccuracy?.toDouble() ?: 30.0,
                Instant.now()
            )
        }

        kalman?.update(
            Instant.now(),
            getXPosition(gps.location).toDouble(),
            getYPosition(gps.location).toDouble(),
            getXVelocity(gps.speed, gps.rawBearing ?: 0f).toDouble(),
            getYVelocity(gps.speed, gps.rawBearing ?: 0f).toDouble(),
            gps.horizontalAccuracy?.toDouble() ?: 30.0,
            gps.speedAccuracy?.toDouble() ?: 1.0
        )

        currentLocation = getLocation(
            kalman?.currentX?.toFloat() ?: 0f,
            kalman?.currentY?.toFloat() ?: 0f
        )
        return true
    }

    private fun isFarFromReference(location: Coordinate): Boolean {
        return location.distanceTo(referenceLocation) > 200
    }

    private fun getXVelocity(speed: Speed, bearing: Float): Float {
        return speed.speed * cosDegrees(Trigonometry.toUnitAngle(bearing, 90f, false))
    }

    private fun getYVelocity(speed: Speed, bearing: Float): Float {
        return speed.speed * sinDegrees(Trigonometry.toUnitAngle(bearing, 90f, false))
    }

    private fun getXPosition(location: Coordinate): Float {
        return referenceProjection.toPixels(location).x
    }

    private fun getYPosition(location: Coordinate): Float {
        return referenceProjection.toPixels(location).y
    }

    private fun getLocation(x: Float, y: Float): Coordinate {
        return referenceProjection.toCoordinate(Vector2(x, y))
    }

    private fun update() {
        if (!gps.hasValidReading || currentLocation == Coordinate.zero || kalman == null) return

        kalman?.predict(
            Instant.now(),
            accelerometer?.rawAcceleration?.get(0)?.toDouble() ?: 0.0,
            accelerometer?.rawAcceleration?.get(1)?.toDouble() ?: 0.0,
        )

        currentLocation = getLocation(
            kalman?.currentX?.toFloat() ?: 0f,
            kalman?.currentY?.toFloat() ?: 0f
        )

        notifyListeners()
    }

}