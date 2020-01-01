package com.kylecorry.survival_aid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.*

/**
 * A sensor
 */
abstract class AbstractSensor(ctx: Context, private val sensorType: Int, private val sensorDelay: Int) : SensorEventListener, Observable(), ISensor {

    private var sensorManager: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var started = false


    /**
     * Start the sensor
     */
    override fun start(){
        if (started) return
        sensorManager.getDefaultSensor(sensorType)?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                sensorDelay
            )
        }
        started = true
    }

    /**
     * Stop the compass sensor
     */
    override fun stop(){
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Empty
    }

    override fun onSensorChanged(event: SensorEvent) {
        handleSensorEvent(event)
        setChanged()
        notifyObservers()
    }

    abstract fun handleSensorEvent(event: SensorEvent)
}