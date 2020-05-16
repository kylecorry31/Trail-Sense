package com.kylecorry.trail_sense.shared.sensors

import android.hardware.SensorManager

class NullBarometer : AbstractSensor(), IBarometer {

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