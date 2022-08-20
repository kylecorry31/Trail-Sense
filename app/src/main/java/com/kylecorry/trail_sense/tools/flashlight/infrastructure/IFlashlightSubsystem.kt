package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState


interface IFlashlightSubsystem {
    val state: ITopic<FlashlightState>
    val brightnessLevels: Int
    fun off()
    fun toggle(handleTimeout: Boolean = true, brightness: Float = 1f)
    fun set(state: FlashlightState, handleTimeout: Boolean = true, brightness: Float = 1f)
    fun getState(): FlashlightState
    fun isAvailable(): Boolean
}