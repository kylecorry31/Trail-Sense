package com.kylecorry.trail_sense.tools.whistle.infrastructure

import java.time.Duration

data class Signal(val on: Boolean, val duration: Duration) {

    companion object {
        fun on(duration: Duration): Signal {
            return Signal(true, duration)
        }

        fun off(duration: Duration): Signal {
            return Signal(false, duration)
        }
    }
}
