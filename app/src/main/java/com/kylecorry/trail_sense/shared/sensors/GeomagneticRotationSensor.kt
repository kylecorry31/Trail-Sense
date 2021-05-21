package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.math.Euler
import com.kylecorry.trailsensecore.domain.math.Quaternion
import com.kylecorry.trailsensecore.domain.math.QuaternionMath
import com.kylecorry.trailsensecore.infrastructure.sensors.BaseSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.ICompass
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.IRotationSensor

class GeomagneticRotationSensor(context: Context, private val useTrueNorth: Boolean) :
    BaseSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST),
    IRotationSensor, ICompass {

    private val lock = Object()

    override val rawEuler: FloatArray
        get() {
            val raw = rawQuaternion
            val euler = FloatArray(3)
            QuaternionMath.toEuler(raw, euler)
            return euler
        }

    override val euler: Euler
        get() = Euler.from(rawEuler)
    override val bearing: Bearing
        get() = Bearing(rawBearing)

    override var declination: Float = 0.0f

    override val hasValidReading: Boolean
        get() = _hasReading
    override val rawBearing: Float
        get(){
            val yaw = rawEuler[2]
            return if (useTrueNorth) {
                Bearing.getBearing(Bearing.getBearing(yaw) + declination)
            } else {
                Bearing.getBearing(yaw)
            }
        }

    override val quaternion: Quaternion
        get() = Quaternion.from(rawQuaternion)

    override val rawQuaternion: FloatArray
        get() {
            return synchronized(lock) {
                _quaternion
            }
        }

    private val _quaternion = Quaternion.zero.toFloatArray()

    private var _hasReading = false

    override fun handleSensorEvent(event: SensorEvent) {
        synchronized(lock) {
            SensorManager.getQuaternionFromVector(_quaternion, event.values)
            val w = _quaternion[0]
            val x = _quaternion[1]
            val y = _quaternion[2]
            val z = _quaternion[3]
            _quaternion[0] = x
            _quaternion[1] = y
            _quaternion[2] = z
            _quaternion[3] = w
            QuaternionMath.inverse(_quaternion, _quaternion)
        }
        _hasReading = true
    }
}