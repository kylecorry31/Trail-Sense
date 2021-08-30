package com.kylecorry.trail_sense.shared.morse

import java.time.Duration

object Signals {

    fun sos(dotDuration: Duration): List<Signal> {
        return listOf(
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration.multipliedBy(3)),
            Signal.off(dotDuration),
            Signal.on(dotDuration.multipliedBy(3)),
            Signal.off(dotDuration),
            Signal.on(dotDuration.multipliedBy(3)),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration)
        )
    }

    fun help(): List<Signal> {
        return listOf(
            Signal.on(Duration.ofSeconds(2)),
            Signal.off(Duration.ofSeconds(1)),
            Signal.on(Duration.ofSeconds(2)),
            Signal.off(Duration.ofSeconds(1)),
            Signal.on(Duration.ofSeconds(2)),
            Signal.off(Duration.ofSeconds(3))
        )
    }

    fun acknowledged(): List<Signal> {
        return listOf(Signal.on(Duration.ofSeconds(2)))
    }

    fun comeHere(): List<Signal> {
        return listOf(
            Signal.on(Duration.ofSeconds(2)),
            Signal.off(Duration.ofSeconds(1)),
            Signal.on(Duration.ofSeconds(2)),
        )
    }

}