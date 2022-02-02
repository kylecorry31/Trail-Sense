package com.kylecorry.trail_sense.shared.sensors

import android.hardware.SensorManager
import com.kylecorry.andromeda.sense.barometer.IBarometer

class NullBarometer : NullSensor(), IBarometer {
    override val pressure: Float
        get() = SensorManager.PRESSURE_STANDARD_ATMOSPHERE

    override val altitude: Float
        get() = 0f
}