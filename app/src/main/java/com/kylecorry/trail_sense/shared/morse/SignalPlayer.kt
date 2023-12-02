package com.kylecorry.trail_sense.shared.morse

import com.kylecorry.andromeda.core.time.CoroutineTimer

class SignalPlayer(private val device: ISignalingDevice) {

    private var intervalometer: CoroutineTimer? = null
    private var isOn = false

    fun play(signals: List<Signal>, loop: Boolean, onComplete: (() -> Any)? = null){
        cancel()
        if (signals.isEmpty()){
            return
        }
        var idx = 0
        isOn = true
        intervalometer = CoroutineTimer {
            synchronized(this) {
                if (!isOn){
                    intervalometer = null
                    return@CoroutineTimer
                }
                if (idx >= signals.size && loop){
                    idx = 0
                }

                if (idx < signals.size) {
                    val signal = signals[idx]
                    if (signal.isOn){
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