package com.kylecorry.trail_sense.shared.morse

import java.time.Duration

data class Signal(val isOn: Boolean, val duration: Duration){

    companion object {
        fun on(duration: Duration): Signal {
            return Signal(true, duration)
        }

        fun off(duration: Duration): Signal {
            return Signal(false, duration)
        }
    }
}