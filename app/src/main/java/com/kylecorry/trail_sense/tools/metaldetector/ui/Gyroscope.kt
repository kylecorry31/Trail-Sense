package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.toDegrees
import com.kylecorry.trailsensecore.domain.math.wrap
import com.kylecorry.trailsensecore.infrastructure.sensors.BaseSensor

class Gyroscope(context: Context): BaseSensor(context, Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST),
    IGyroscope {

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

    private val _rotation = FloatArray(3)

    private val NS2S = 1.0f / 1000000000.0f

    override val hasValidReading: Boolean
        get() = _hasReading

    private var _hasReading = false
    private var lastTime = 0L

    private val lock = Object()

    override fun handleSensorEvent(event: SensorEvent) {
        if (event.values.size < 3){
            return
        }

        if (lastTime == 0L){
            lastTime = event.timestamp
            return
        }
        val dt = (event.timestamp - lastTime) * NS2S
        lastTime = event.timestamp

        synchronized(lock) {
            _rotation[0] += (event.values[0] * dt).toDegrees()
            _rotation[1] += (event.values[1] * dt).toDegrees()
            _rotation[2] += (event.values[2] * dt).toDegrees()
        }

        _hasReading = true
    }


    override fun calibrate(){
        synchronized(lock) {
            _rotation[0] = 0f
            _rotation[1] = 0f
            _rotation[2] = 0f
        }
    }


}