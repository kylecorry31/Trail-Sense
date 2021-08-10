package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.torch.ITorch
import com.kylecorry.trailsensecore.infrastructure.morse.ISignalingDevice

fun ITorch.asSignal(): ISignalingDevice {
    val torch = this
    return object : ISignalingDevice {
        override fun off() {
            torch.off()
        }

        override fun on() {
            torch.on()
        }
    }
}