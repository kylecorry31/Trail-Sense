package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.pedometer.IPedometer

class NullPedometer : NullSensor(), IPedometer {
    override val steps: Int = 0
}