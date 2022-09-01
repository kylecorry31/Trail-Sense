package com.kylecorry.trail_sense.tools.flashlight.infrastructure

class TorchFlashlightStrategy(private val flashlight: FlashlightSubsystem) : IFlashlightStrategy {
    override fun start() {
        flashlight.turnOn()
    }

    override fun stop() {
        flashlight.turnOff()
    }
}