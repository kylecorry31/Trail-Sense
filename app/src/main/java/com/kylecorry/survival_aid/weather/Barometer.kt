package com.kylecorry.survival_aid.weather

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.survival_aid.AbstractSensor
import kotlin.math.pow

/**
 * A barometer sensor
 */
class Barometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL) {

    /**
     * The temperature in hPa
     */
    var pressure: Float = 0f
        private set

    /**
     * Get the pressure reading at sea level
     * @param altitude The altitude in meters of the reading
     * @return The pressure at sea level in hPa
     */
    fun getSeaLevelPressure(altitude: Float): Float {
        val pressureInHg = WeatherUtils.hPaToInches(pressure)
        val seaLevelInHg = pressureInHg * ((288 - 0.0065 * altitude) / 288).pow(5.2561)
        return WeatherUtils.inchesToHPa(seaLevelInHg.toFloat())
    }

    override fun handleSensorEvent(event: SensorEvent) {
        pressure = event.values[0]
    }
}