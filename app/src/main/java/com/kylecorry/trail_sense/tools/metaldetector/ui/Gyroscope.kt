package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.toDegrees
import com.kylecorry.trailsensecore.domain.math.wrap
import com.kylecorry.trailsensecore.infrastructure.sensors.BaseSensor
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Gyroscope(context: Context) :
    BaseSensor(context, Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST),
    IGyroscope {

    // TODO: Use quaternions
    override val rawRotation: FloatArray
        get() {
            return synchronized(lock) {
                floatArrayOf(
                    wrap(_rotation[0], 0f, 360f),
                    wrap(_rotation[1], 0f, 360f),
                    wrap(_rotation[2], 0f, 360f)
                )
            }
        }

    override val rotation: Vector3
        get() {
            return synchronized(lock) {
                Vector3(
                    wrap(_rotation[0], 0f, 360f),
                    wrap(_rotation[1], 0f, 360f),
                    wrap(_rotation[2], 0f, 360f)
                )
            }
        }

    override val quaternion: Quaternion
        get() = Quaternion.from(rawQuaternion)

    override val rawQuaternion: FloatArray
        get() {
            return synchronized(lock) {
                _quaternion.clone()
            }
        }

    private val _rotation = FloatArray(3)
    private val _quaternion = Quaternion.zero.toFloatArray()

    private val NS2S = 1.0f / 1000000000.0f

    override val hasValidReading: Boolean
        get() = _hasReading

    private var _hasReading = false
    private var lastTime = 0L

    private val lock = Object()

    override fun handleSensorEvent(event: SensorEvent) {
        if (event.values.size < 3) {
            return
        }

        if (lastTime == 0L) {
            lastTime = event.timestamp
            return
        }
        val dt = (event.timestamp - lastTime) * NS2S
        lastTime = event.timestamp


        var axisX = -event.values[0]
        var axisY = event.values[1]
        var axisZ = -event.values[2]

        // Calculate the angular speed of the sample
        val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

        // Normalize the rotation vector if it's big enough to get the axis
        // (that is, EPSILON should represent your maximum allowable margin of error)
        if (omegaMagnitude > 0.00000001f) {
            axisX /= omegaMagnitude
            axisY /= omegaMagnitude
            axisZ /= omegaMagnitude
        }

        val thetaOverTwo: Float = omegaMagnitude * dt / 2.0f
        val sinThetaOverTwo: Float = sin(thetaOverTwo)
        val cosThetaOverTwo: Float = cos(thetaOverTwo)
        val deltaRotationVector = FloatArray(4)
        deltaRotationVector[0] = sinThetaOverTwo * axisX
        deltaRotationVector[1] = sinThetaOverTwo * axisY
        deltaRotationVector[2] = sinThetaOverTwo * axisZ
        deltaRotationVector[3] = cosThetaOverTwo

        synchronized(lock) {
            _rotation[0] += (event.values[0] * dt).toDegrees()
            _rotation[1] += (event.values[1] * dt).toDegrees()
            _rotation[2] += (event.values[2] * dt).toDegrees()
            QuaternionMath.multiply(_quaternion, deltaRotationVector, _quaternion)
            QuaternionMath.normalize(_quaternion, _quaternion)
        }

        _hasReading = true
    }


    override fun calibrate() {
        synchronized(lock) {
            _rotation[0] = 0f
            _rotation[1] = 0f
            _rotation[2] = 0f
            _quaternion[0] = 0f
            _quaternion[1] = 0f
            _quaternion[2] = 0f
            _quaternion[3] = 1f
        }
    }


}