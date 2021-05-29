package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.os.SystemClock
import com.kylecorry.trailsensecore.domain.math.*
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.Accelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.IAccelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.LowPassAccelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.IMagnetometer
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.LowPassMagnetometer
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.Magnetometer
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.Gyroscope
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.IGyroscope
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.IOrientationSensor
import kotlin.math.absoluteValue

// Adapted from https://github.com/xioTechnologies/Fusion
class MadgwickAHRS(
    private val context: Context,
    gain: Float = 2f,
    private val accelerometer: IAccelerometer? = null,
    private val gyro: IGyroscope? = null,
    private val magnetometer: IMagnetometer? = null
) : AbstractSensor(),
    IOrientationSensor {

    private val _orientation = Quaternion.zero.toFloatArray()

    private val _accelerometer: IAccelerometer by lazy {
        accelerometer ?: LowPassAccelerometer(context)
    }
    private val _gyro by lazy { gyro ?: Gyroscope(context) }
    private val _magnetometer: IMagnetometer by lazy {
        magnetometer ?: LowPassMagnetometer(context)
    }

    private val lock = Object()

    override val orientation
        get() = Quaternion.from(rawOrientation)

    override val rawOrientation: FloatArray
        get() {
            return synchronized(lock) {
                _orientation.clone()
            }
        }


    private var hasMag = false
    private var hasAcc = false
    private var hasGyr = false
    private val NS2S = 1.0f / 1000000000.0f

    private var madgwick = Madgwick(gain)

    override val hasValidReading: Boolean
        get() = hasReading

    private var hasReading = false

    private var lastTime = 0L
    private var dt = 0f

    private fun onSensorUpdate() {
        if (!hasGyr || !hasAcc || !hasMag) {
            return
        }

        madgwick.update(
            Euler(-_gyro.angularRate.pitch, -_gyro.angularRate.roll, _gyro.angularRate.yaw),
            Vector3(
                _magnetometer.magneticField.y,
                _magnetometer.magneticField.x,
                _magnetometer.magneticField.z
            ),
            Vector3(
                _accelerometer.acceleration.y,
                _accelerometer.acceleration.x,
                _accelerometer.acceleration.z
            ),
            dt
        )

        synchronized(lock) {
            madgwick.quaternion.copyInto(_orientation)
        }

        hasReading = true

        notifyListeners()
    }

    private fun onAccelUpdate(): Boolean {
        hasAcc = true
        return true
    }

    private fun onMagUpdate(): Boolean {
        hasMag = true
        return true
    }

    private fun onGyroUpdate(): Boolean {
        val time = SystemClock.elapsedRealtimeNanos()
        if (lastTime == 0L) {
            lastTime = time
            return true
        }
        dt = (time - lastTime) * NS2S
        lastTime = time
        hasGyr = true
        onSensorUpdate()
        return true
    }

    override fun startImpl() {
        _accelerometer.start(this::onAccelUpdate)
        _magnetometer.start(this::onMagUpdate)
        _gyro.start(this::onGyroUpdate)
    }

    override fun stopImpl() {
        _accelerometer.stop(this::onAccelUpdate)
        _magnetometer.stop(this::onMagUpdate)
        _gyro.stop(this::onGyroUpdate)
    }

    private class Madgwick(private val gain: Float = 0.1f) {

        private val lock = Object()
        private val halfGyro = FloatArray(3)
        private val halfGravity = FloatArray(3)
        private val halfWest = FloatArray(3)
        private val gyroQ = FloatArray(4)
        val halfFeedbackError = FloatArray(3)

        var quaternion = Quaternion.zero.toFloatArray()
            get() = synchronized(lock) {
                field
            }


        fun update(g: Euler, m: Vector3, a: Vector3, dt: Float) {
            synchronized(lock) {
                val qx: Float = quaternion[0]
                val qy: Float = quaternion[1]
                val qz: Float = quaternion[2]
                val qw: Float = quaternion[3]

                // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
                if (a != Vector3.zero) {
                    halfGravity[0] = qx * qz - qw * qy
                    halfGravity[1] = qw * qx + qy * qz
                    halfGravity[2] = qw * qw - 0.5f + qz * qz
                    val normalA = a.normalize()
                    Vector3Utils.cross(normalA.toFloatArray(), halfGravity)
                        .copyInto(halfFeedbackError)

                    if (m.magnitude().absoluteValue > 10f) {
                        halfWest[0] = qx * qy + qw * qz
                        halfWest[1] = qw * qw - 0.5f + qy * qy
                        halfWest[2] = qy * qz - qw * qx

                        Vector3Utils.plus(
                            halfFeedbackError, Vector3Utils.cross(
                                Vector3Utils.normalize(
                                    Vector3Utils.cross(a.toFloatArray(), m.toFloatArray())
                                ),
                                halfWest
                            )
                        ).copyInto(halfFeedbackError)
                    }
                }

                val gx = g.roll.toRadians() * 0.5f
                val gy = g.pitch.toRadians() * 0.5f
                val gz = g.yaw.toRadians() * 0.5f

                halfGyro[0] = gx
                halfGyro[1] = gy
                halfGyro[2] = gz

                Vector3Utils.plus(halfGyro, Vector3Utils.times(halfFeedbackError, gain))
                    .copyInto(halfGyro)

                gyroQ[0] = halfGyro[0] * dt
                gyroQ[1] = halfGyro[1] * dt
                gyroQ[2] = halfGyro[2] * dt
                gyroQ[3] = 0f
                QuaternionMath.multiply(quaternion, gyroQ, gyroQ)
                QuaternionMath.add(quaternion, gyroQ, quaternion)
                QuaternionMath.normalize(quaternion, quaternion)
            }
        }

    }

}