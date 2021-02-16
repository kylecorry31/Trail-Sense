package com.kylecorry.trail_sense.tools.whistle.infrastructure

import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class SignalPlayer(private val device: ISignalingDevice) {

    private var intervalometer: Intervalometer? = null
    private var isOn = false;

    fun play(signals: List<Signal>, loop: Boolean, onComplete: (() -> Any)? = null){
        cancel()
        if (signals.isEmpty()){
            return
        }
        var idx = 0
        isOn = true
        intervalometer = Intervalometer {
            synchronized(this) {
                if (!isOn){
                    intervalometer = null
                    return@Intervalometer
                }
                if (idx >= signals.size && loop){
                    idx = 0
                }

                if (idx < signals.size) {
                    val signal = signals[idx]
                    if (signal.on){
                        device.on()
                    } else {
                        device.off()
                    }
                    idx++
                    intervalometer?.once(signal.duration)
                } else {
                    intervalometer = null
                    onComplete?.invoke()
                }
            }
        }
        intervalometer?.once(0)
    }

    fun cancel(){
        synchronized(this) {
            isOn = false
            if (intervalometer != null) {
                intervalometer?.stop()
                intervalometer = null
            }
            device.off()
        }
    }

}