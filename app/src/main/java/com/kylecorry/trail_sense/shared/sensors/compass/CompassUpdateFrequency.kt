package com.kylecorry.trail_sense.shared.sensors.compass

import android.hardware.SensorManager

enum class CompassUpdateFrequency(val id: String, val sensorDelay: Int) {
    Normal("normal", SensorManager.SENSOR_DELAY_UI),
    Fast("fast", SensorManager.SENSOR_DELAY_GAME)
}
