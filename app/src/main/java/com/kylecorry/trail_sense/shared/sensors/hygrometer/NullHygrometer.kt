package com.kylecorry.trail_sense.shared.sensors.hygrometer

import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.hygrometer.IHygrometer

class NullHygrometer : AbstractSensor(), IHygrometer {

    override val hasValidReading: Boolean
        get() = false

    override val humidity: Float
        get() = _humidity

    private var _humidity = Float.NaN

    override fun startImpl() {
        notifyListeners()
    }

    override fun stopImpl() {
    }

}