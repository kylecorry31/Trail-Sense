package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.withSign

class DeviceOrientation(context: Context) :
    BaseSensor(context, Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_UI) {

    override val hasValidReading: Boolean
        get() = gotReading

    var orientation: Orientation = Orientation.Flat
        private set

    private var gotReading = false

    override fun handleSensorEvent(event: SensorEvent) {
        val acceleration = event.values
        var largestAccelAxis = 0
        for (i in acceleration.indices) {
            if (abs(acceleration[i]) > abs(acceleration[largestAccelAxis])) {
                largestAccelAxis = i
            }
        }

        largestAccelAxis = (largestAccelAxis + 1).toDouble()
            .withSign(acceleration[largestAccelAxis].toDouble()).toInt()

        orientation = when (largestAccelAxis) {
            -3 -> Orientation.FlatInverse
            -2 -> Orientation.PortraitInverse
            -1 -> Orientation.LandscapeInverse
            1 -> Orientation.Landscape
            2 -> Orientation.Portrait
            else -> Orientation.Flat
        }

        gotReading = true
    }

    enum class Orientation {
        Portrait,
        PortraitInverse,
        Flat,
        FlatInverse,
        Landscape,
        LandscapeInverse
    }

}