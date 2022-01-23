package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import java.time.Instant

interface IStepCounter {
    val startTime: Instant?
    val steps: Long

    fun addSteps(steps: Long)
    fun reset()
}