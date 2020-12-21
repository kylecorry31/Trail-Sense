package com.kylecorry.trail_sense.shared

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import java.time.Duration

class Vibrator(context: Context) {

    private val vibrator by lazy { context.getSystemService<Vibrator>() }

    fun vibrate(duration: Duration) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(LongArray(3) {
                when (it) {
                    0 -> 0
                    else -> duration.toMillis()
                }
            }, 0))
        } else {
            vibrator?.vibrate(LongArray(3) {
                when (it) {
                    0 -> 0
                    else -> duration.toMillis()
                }
            }, 0)
        }
    }

    fun stop() {
        vibrator?.cancel()
    }


}