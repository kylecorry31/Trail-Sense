package com.kylecorry.trail_sense.sensors.barometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.sensorfilters.KalmanFilter
import com.kylecorry.trail_sense.models.PressureReading
import com.kylecorry.trail_sense.sensors.AbstractSensor
import java.time.Instant

/**
 * A barometer sensor
 */
class Barometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL),
    IBarometer {

    private var _pressure: PressureReading = PressureReading(Instant.MIN, 0F)
    private val filter = KalmanFilter(0.5, 0.01)

    override val pressure: PressureReading
        get() = _pressure

    override fun handleSensorEvent(event: SensorEvent) {
        _pressure = PressureReading(Instant.now(), filter.filter(event.values[0].toDouble()).toFloat())
    }
}