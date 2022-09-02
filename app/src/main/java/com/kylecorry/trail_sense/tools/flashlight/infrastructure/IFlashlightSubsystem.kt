package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode


interface IFlashlightSubsystem {
    val mode: ITopic<FlashlightMode>
    val brightnessLevels: Int
    fun setBrightness(brightness: Float)
    fun toggle()
    fun set(mode: FlashlightMode)
    fun getMode(): FlashlightMode
    fun isAvailable(): Boolean
}