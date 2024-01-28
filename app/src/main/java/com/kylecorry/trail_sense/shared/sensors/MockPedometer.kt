package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.andromeda.sense.pedometer.IPedometer

class MockPedometer : MockSensor(), IPedometer {
    override val steps: Int = 0
}