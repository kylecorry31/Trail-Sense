package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.trail_sense.shared.morse.ISignalingDevice
import com.kylecorry.trail_sense.shared.morse.Signal
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals
import java.time.Duration

class SosFlashlightStrategy(private val flashlight: FlashlightSubsystem) : IFlashlightStrategy {

    private val player by lazy {
        SignalPlayer(
            ISignalingDevice.from(
                flashlight::turnOn,
                flashlight::turnOff
            )
        )
    }

    override fun start() {
        val sos = Signals.sos(Duration.ofMillis(200)) + listOf(
            Signal.off(Duration.ofMillis(200L * 7))
        )
        player.play(sos, true)
    }

    override fun stop() {
        player.cancel()
        flashlight.turnOff()
    }
}