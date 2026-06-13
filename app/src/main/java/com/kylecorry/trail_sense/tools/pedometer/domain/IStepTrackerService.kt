package com.kylecorry.trail_sense.tools.pedometer.domain

import java.time.Instant

interface IStepTrackerService {
    suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod>

    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod?

    suspend fun startNewStepTrackingPeriod(endTime: Instant = Instant.now())

    suspend fun addSteps(steps: Long, time: Instant = Instant.now())

    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod)
}
