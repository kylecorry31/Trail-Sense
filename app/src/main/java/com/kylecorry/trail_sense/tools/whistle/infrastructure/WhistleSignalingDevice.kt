package com.kylecorry.trail_sense.tools.whistle.infrastructure

import com.kylecorry.trailsensecore.infrastructure.audio.ISoundPlayer

class WhistleSignalingDevice(private val whistle: ISoundPlayer) : ISignalingDevice {
    override fun on() {
        whistle.on()
    }

    override fun off() {
        whistle.off()
    }
}