package com.kylecorry.survival_aid.flashlight

import com.kylecorry.survival_aid.flashlight.MorseEncoder.MorseSymbol.*
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToLong

interface FlashlightMode {
    /**
     * Activate the flashlight
     */
    fun on(flashlight: Flashlight)

    /**
     * Deactivate the flashlight
     */
    fun off(flashlight: Flashlight)
}

/**
 * A normal flashlight mode
 */
class NormalFlashlightMode: FlashlightMode {
    override fun on(flashlight: Flashlight) {
        flashlight.bulbOn()
    }

    override fun off(flashlight: Flashlight) {
        flashlight.bulbOff()
    }
}

/**
 * A strobe flashlight mode
 * @param frequency the frequency in Hz to strobe
 */
class StrobeFlashlightMode(private val frequency: Float): FlashlightMode {

    private val states = listOf(true, false)
    private val pattern = PatternFlashlightMode(states, (1 / frequency / states.size * 1000).roundToLong(), true)

    override fun on(flashlight: Flashlight) {
        pattern.on(flashlight)
    }

    override fun off(flashlight: Flashlight) {
        pattern.off(flashlight)
    }

}

/**
 * A SOS morse code flashlight mode
 * @param dotDuration the duration of a dot in milliseconds
 */
class SosFlashlightMode(dotDuration: Long): FlashlightMode {

    private val sos = MorseEncoder.encode(listOf(
        DOT, // S
        DOT,
        DOT,
        SPACE,
        DASH, // O
        DASH,
        DASH,
        SPACE,
        DOT, // S
        DOT,
        DOT,
        WORD_SEPARATOR
    ))
    private val pattern = PatternFlashlightMode(sos, dotDuration, true)

    override fun on(flashlight: Flashlight) {
        pattern.on(flashlight)
    }

    override fun off(flashlight: Flashlight) {
        pattern.off(flashlight)
    }
}

/**
 * A flashlight mode which displays a pattern
 */
private class PatternFlashlightMode(private val steps: List<Boolean>, private val stepDuration: Long, private val repeat: Boolean = true): FlashlightMode {
    private var timer: Timer? = null

    override fun on(flashlight: Flashlight) {
        timer?.cancel()
        var idx = 0
        timer = fixedRateTimer(period = stepDuration, action = {
            if (!repeat && idx >= steps.size){
                off(flashlight)
            } else {
                idx %= steps.size
                val state = steps[idx]
                idx++
                if (state) flashlight.bulbOn()
                else flashlight.bulbOff()
            }
        })
    }

    override fun off(flashlight: Flashlight) {
        timer?.cancel()
        flashlight.bulbOff()
    }
}