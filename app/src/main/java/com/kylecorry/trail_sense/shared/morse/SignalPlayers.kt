package com.kylecorry.trail_sense.shared.morse

import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.andromeda.torch.ITorch

fun ITorch.asSignal(brightness: Float = 1f): ISignalingDevice {
    return ISignalingDevice.from({ on(brightness) }, this::off)
}

fun ISoundPlayer.asSignal(): ISignalingDevice {
    return ISignalingDevice.from(this::on, this::off)
}