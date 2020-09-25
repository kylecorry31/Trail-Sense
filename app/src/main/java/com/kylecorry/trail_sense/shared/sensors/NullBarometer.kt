package com.kylecorry.trail_sense.shared.sensors

import android.hardware.SensorManager
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer

class NullBarometer : AbstractSensor(), IBarometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = true

    override val pressure: Float
        get() = SensorManager.PRESSURE_STANDARD_ATMOSPHERE

    override val altitude: Float
        get() = 0f

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }

}