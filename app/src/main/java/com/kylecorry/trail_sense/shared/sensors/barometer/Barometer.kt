package com.kylecorry.trail_sense.shared.sensors.barometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.shared.PressureReading
import com.kylecorry.trail_sense.shared.sensors.AbstractSensor
import java.time.Instant

/**
 * A barometer sensor
 */
class Barometer(ctx: Context) : AbstractSensor(ctx, Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL),
    IBarometer {

    private var _pressure: PressureReading =
        PressureReading(Instant.MIN, 0F)

    override val pressure: PressureReading
        get() = _pressure

    override fun handleSensorEvent(event: SensorEvent) {
        _pressure = PressureReading(
            Instant.now(),
            event.values[0]
        )
    }
}