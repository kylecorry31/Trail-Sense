package com.kylecorry.survival_aid.flashlight

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

    private var timer: Timer? = null

    override fun on(flashlight: Flashlight) {
        timer?.cancel()
        val period: Long = (1 / frequency * 1000).roundToLong()
        timer = fixedRateTimer(period = period, action = {
            if (flashlight.isBulbOn) flashlight.bulbOff()
            else flashlight.bulbOn()
        })
    }

    override fun off(flashlight: Flashlight) {
        timer?.cancel()
        flashlight.bulbOff()
    }

}