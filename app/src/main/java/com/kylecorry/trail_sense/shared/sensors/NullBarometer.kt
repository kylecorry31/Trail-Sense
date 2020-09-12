package com.kylecorry.trail_sense.shared.sensors

import android.hardware.SensorManager
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer

class NullBarometer : AbstractSensor(), IBarometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = true

    override val pressure: Float
        get() = _pressure

    override val altitude: Float
        get() = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)

    private var _pressure = 0f

    override fun startImpl() {
        _pressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE
        notifyListeners()
    }

    override fun stopImpl() {
    }

}