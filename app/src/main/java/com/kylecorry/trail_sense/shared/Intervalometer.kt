package com.kylecorry.trail_sense.shared

import android.os.Handler
import android.os.Looper
import java.time.Duration

class Intervalometer(private val runnable: Runnable) {

    private var running = false
    private val handler = Handler(Looper.getMainLooper())

    private var intervalRunnable: Runnable? = null


    fun interval(periodMillis: Long, initialDelayMillis: Long = 0L) {
        if (running) {
            stop()
        }

        running = true

        val r = Runnable {
            runnable.run()
            val nextRunnable = intervalRunnable
            if (nextRunnable != null) {
                handler.postDelayed(nextRunnable, periodMillis)
            }
        }

        intervalRunnable = r

        handler.postDelayed(r, initialDelayMillis)
    }

    fun interval(period: Duration, initialDelay: Duration = Duration.ZERO) {
        interval(period.toMillis(), initialDelay.toMillis())
    }

    fun once(delayMillis: Long) {
        if (running) {
            stop()
        }

        running = true
        handler.postDelayed(runnable, delayMillis)
    }

    fun once(delay: Duration) {
        once(delay.toMillis())
    }

    fun stop() {
        val iRunnable = intervalRunnable
        if (iRunnable != null) {
            handler.removeCallbacks(iRunnable)
        }
        intervalRunnable = null

        handler.removeCallbacks(runnable)
        running = false
    }

    fun isRunning(): Boolean {
        return running
    }

}