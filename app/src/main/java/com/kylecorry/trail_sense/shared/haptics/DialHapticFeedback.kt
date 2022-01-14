package com.kylecorry.trail_sense.shared.haptics

import android.content.Context
import com.kylecorry.andromeda.buzz.Buzz
import com.kylecorry.andromeda.buzz.HapticFeedbackType
import com.kylecorry.sol.math.SolMath.deltaAngle
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class DialHapticFeedback(private val context: Context, private val vibrateFrequency: Int = 1) {

    private var lastVibrate = 0f

    var angle: Float = 0f
        set(value) {
            if (shouldVibrate(value, lastVibrate)) {
                Buzz.feedback(context, HapticFeedbackType.Tick)
                lastVibrate = value
            }
            field = value
        }

    private fun shouldVibrate(current: Float, last: Float): Boolean {
        val currInt = current.roundToInt() % 360
        val lastInt = last.roundToInt() % 360

        return currInt % vibrateFrequency == 0 && currInt != lastInt && deltaAngle(current, last).absoluteValue > 0.25f
    }


    fun stop() {
        Buzz.off(context)
    }

}