package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.math.*
import com.kylecorry.trail_sense.shared.toDegrees
import com.kylecorry.trail_sense.weather.domain.MovingAverageFilter
import kotlin.math.atan2
import kotlin.math.min

// From https://stackoverflow.com/questions/16317599/android-compass-that-can-compensate-for-tilt-and-pitch

class VectorCompass(context: Context) : AbstractSensor(), ICompass {

    override val accuracy: Accuracy
        get() = _accuracy
    private var _accuracy: Accuracy = Accuracy.Unknown

    // TODO: Check if gravity sensor is available, else use accelerometer
    private val sensorChecker = SensorChecker(context)
    private val accelerometer: IAccelerometer = if (sensorChecker.hasGravity()) GravitySensor(context) else LowPassAccelerometer(context)
    private val magnetometer = Magnetometer(context)

    private val prefs = UserPreferences(context)
    private val filterSize = prefs.navigation.compassSmoothing * 2 * 2
    private val filter = MovingAverageFilter(filterSize)

    override var declination = 0f

    override val bearing: Bearing
        get() = Bearing(_filteredBearing).withDeclination(declination)

    private var _bearing = 0f
    private var _filteredBearing = 0f

    private var gotMag = false;
    private var gotAccel = false;

    private fun updateBearing(newBearing: Float) {
        _bearing += deltaAngle(_bearing, newBearing)
        _filteredBearing = filter.filter(_bearing.toDouble()).toFloat()
    }

    private fun updateSensor(): Boolean {

        if (!gotAccel || !gotMag) {
            return true
        }

        // Gravity
        val normGravity = accelerometer.acceleration.normalize()
        val normMagField = magnetometer.magneticField.normalize()

        val accelAccuracy = accelerometer.accuracy
        val magAccuracy = magnetometer.accuracy

        _accuracy = Accuracy.values()[min(accelAccuracy.ordinal, magAccuracy.ordinal)]

        // East vector
        val east = normMagField.cross(normGravity)
        val normEast = east.normalize()

        // Magnitude check
        val eastMagnitude = east.magnitude()
        val gravityMagnitude = accelerometer.acceleration.magnitude()
        val magneticMagnitude = magnetometer.magneticField.magnitude()
        if (gravityMagnitude * magneticMagnitude * eastMagnitude < 0.1f) {
            return true
        }

        // North vector
        val dotProduct = normGravity.dot(normMagField)
        val north = normMagField.minus(normGravity * dotProduct)
        val normNorth = north.normalize()

        // Azimuth
        // NB: see https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
        val sin = normEast.y - normNorth.x
        val cos = normEast.x + normNorth.y
        val azimuth = if (sin != 0f && cos != 0f) atan2(sin, cos) else 0f

        if (azimuth.isNaN()){
            return true
        }

        updateBearing(azimuth.toDegrees())
        notifyListeners()
        return true
    }

    private fun updateAccel(): Boolean {
        gotAccel = true
        return updateSensor()
    }

    private fun updateMag(): Boolean {
        gotMag = true
        return updateSensor()
    }

    override fun startImpl() {
        accelerometer.start(this::updateAccel)
        magnetometer.start(this::updateMag)
    }

    override fun stopImpl() {
        accelerometer.stop(this::updateAccel)
        magnetometer.stop(this::updateMag)
    }

}