package com.kylecorry.survival_aid.flashlight

import android.content.Context
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToLong

class Strobe(private val flashlight: Flashlight): Observable() {

    private var timer: Timer? = null

    var isOn = false
        private set(value){
            field = value
            setChanged()
            notifyObservers()
        }

    fun start(frequency: Float){
        timer?.cancel()
        val period: Long = (1 / frequency * 1000).roundToLong()
        timer = fixedRateTimer(period = period, action = {
            if (flashlight.isOn) flashlight.off()
            else flashlight.on()
        })
        isOn = true
    }

    fun stop(){
        timer?.cancel()
        flashlight.off()
        isOn = false
    }


}