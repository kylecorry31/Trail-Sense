package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState


interface IFlashlightHandler {
    fun initialize()
    fun release()
    fun on()
    fun off()
    fun sos()
    fun strobe()
    fun set(state: FlashlightState)
    fun getState(): FlashlightState
    fun isAvailable(): Boolean
}