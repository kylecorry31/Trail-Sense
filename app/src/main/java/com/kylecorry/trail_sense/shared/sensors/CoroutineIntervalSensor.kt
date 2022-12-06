package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.trail_sense.shared.extensions.onMain
import java.time.Duration

abstract class CoroutineIntervalSensor(private val frequency: Duration) : AbstractSensor() {

    private val timer = Timer { update() }

    override val hasValidReading: Boolean
        get() = true

    override fun startImpl() {
        timer.interval(frequency)
    }

    override fun stopImpl() {
        timer.stop()
    }

    protected open suspend fun update() {
        onMain {
            notifyListeners()
        }
    }

}