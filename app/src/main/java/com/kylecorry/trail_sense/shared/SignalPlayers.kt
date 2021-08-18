package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.andromeda.torch.ITorch
import com.kylecorry.trailsensecore.infrastructure.morse.ISignalingDevice

fun ITorch.asSignal(): ISignalingDevice {
    return ISignalingDevice.from(this::on, this::off)
}

fun ISoundPlayer.asSignal(): ISignalingDevice {
    return ISignalingDevice.from(this::on, this::off)
}