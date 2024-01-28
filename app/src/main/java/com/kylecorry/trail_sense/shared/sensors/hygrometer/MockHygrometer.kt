package com.kylecorry.trail_sense.shared.sensors.hygrometer

import com.kylecorry.andromeda.sense.hygrometer.IHygrometer
import com.kylecorry.andromeda.sense.mock.MockSensor

class MockHygrometer : MockSensor(), IHygrometer {
    override val humidity: Float
        get() = _humidity

    private var _humidity = 0f
}