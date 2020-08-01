package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.shared.domain.Accuracy

abstract class BaseSensor(context: Context, private val sensorType: Int, private val sensorDelay: Int): AbstractSensor() {

    override val accuracy: Accuracy
        get() = _accuracy

    private var _accuracy: Accuracy = Accuracy.Unknown

    private val sensorManager = context.getSystemService<SensorManager>()
    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            _accuracy = when(accuracy){
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Accuracy.Low
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Accuracy.Medium
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Accuracy.High
                else -> Accuracy.Unknown
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            handleSensorEvent(event)
            _accuracy = when(event.accuracy){
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Accuracy.Low
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Accuracy.Medium
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Accuracy.High
                else -> Accuracy.Unknown
            }
            notifyListeners()
        }

    }

    override fun startImpl() {
        sensorManager?.getDefaultSensor(sensorType)?.also { sensor ->
            sensorManager.registerListener(
                sensorListener,
                sensor,
                sensorDelay
            )
        }
    }

    override fun stopImpl() {
        sensorManager?.unregisterListener(sensorListener)
    }

    protected abstract fun handleSensorEvent(event: SensorEvent)
}