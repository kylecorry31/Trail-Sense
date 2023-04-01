package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.time.Timer
import java.time.Duration

class TimerPool {

    private val timers = mutableListOf<TimerDetails>()
    private var maxId = 0L

    private var isStarted = false

    private val lock = Any()

    fun add(interval: Duration, action: suspend () -> Unit): Long {
        val timer = Timer(action = action)
        return synchronized(lock) {
            val id = maxId++
            timers.add(TimerDetails(id, timer, interval))
            if (isStarted) {
                timer.interval(interval)
            }
            id
        }
    }

    fun remove(id: Long) {
        synchronized(lock) {
            timers.removeAll { it.id == id }
        }
    }

    fun start() {
        synchronized(lock) {
            isStarted = true
            timers.forEach {
                it.timer.interval(it.interval)
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            isStarted = false
            timers.forEach {
                it.timer.stop()
            }
        }
    }


    private data class TimerDetails(val id: Long, val timer: Timer, val interval: Duration)

}