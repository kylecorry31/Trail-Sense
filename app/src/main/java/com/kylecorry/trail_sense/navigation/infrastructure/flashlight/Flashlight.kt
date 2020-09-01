package com.kylecorry.trail_sense.navigation.infrastructure.flashlight

import android.content.Context
import com.kylecorry.trail_sense.navigation.domain.FlashlightState

class Flashlight(private val context: Context) : IFlashlight {

    override fun on() {
        SosService.stop(context)
        FlashlightService.start(context)
    }

    override fun off() {
        SosService.stop(context)
        FlashlightService.stop(context)
    }

    override fun sos() {
        SosService.start(context)
        FlashlightService.stop(context)
    }

    override fun set(state: FlashlightState) {
        when (state) {
            FlashlightState.Off -> off()
            FlashlightState.On -> on()
            FlashlightState.SOS -> sos()
        }
    }

    override fun getState(): FlashlightState {
        return when {
            FlashlightService.isOn(context) -> FlashlightState.On
            SosService.isOn(context) -> FlashlightState.SOS
            else -> FlashlightState.Off
        }
    }

    override fun getNextState(currentState: FlashlightState?): FlashlightState {
        return when (currentState ?: getState()) {
            FlashlightState.On -> FlashlightState.SOS
            FlashlightState.SOS -> FlashlightState.Off
            FlashlightState.Off -> FlashlightState.On
        }
    }

}