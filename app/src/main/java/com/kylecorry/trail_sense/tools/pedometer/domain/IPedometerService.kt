package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface IPedometerService {
    suspend fun addSteps(steps: Int, time: Instant = Instant.now())
    fun getSteps(): Flow<List<StepSession>>

    suspend fun reset()
}