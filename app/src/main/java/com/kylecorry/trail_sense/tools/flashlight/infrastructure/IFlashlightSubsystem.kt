package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState


interface IFlashlightSubsystem {
    val state: ITopic<FlashlightState>
    fun on(handleTimeout: Boolean = true)
    fun off()
    fun toggle(handleTimeout: Boolean = true)
    fun sos(handleTimeout: Boolean = true)
    fun strobe(handleTimeout: Boolean = true)
    fun set(state: FlashlightState, handleTimeout: Boolean = true)
    fun getState(): FlashlightState
    fun isAvailable(): Boolean
}