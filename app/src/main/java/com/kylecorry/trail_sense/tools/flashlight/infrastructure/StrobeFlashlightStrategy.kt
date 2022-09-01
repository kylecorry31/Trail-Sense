package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.andromeda.core.time.Timer
import java.time.Duration

class StrobeFlashlightStrategy(
    private val flashlight: FlashlightSubsystem,
    private val interval: Duration
) : IFlashlightStrategy {

    private var on = false
    private val timer = Timer {
        if (on) {
            flashlight.turnOff()
        } else {
            flashlight.turnOn()
        }

        on = !on
    }

    override fun start() {
        timer.interval(interval)
    }

    override fun stop() {
        timer.stop()
        flashlight.turnOff()
    }
}