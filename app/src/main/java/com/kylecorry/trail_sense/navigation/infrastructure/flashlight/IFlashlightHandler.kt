package com.kylecorry.trail_sense.navigation.infrastructure.flashlight

import com.kylecorry.trail_sense.navigation.domain.FlashlightState

interface IFlashlightHandler {
    fun initialize()
    fun release()
    fun on()
    fun off()
    fun sos()
    fun set(state: FlashlightState)
    fun getState(): FlashlightState
    fun getNextState(currentState: FlashlightState? = null): FlashlightState
    fun isAvailable(): Boolean
}