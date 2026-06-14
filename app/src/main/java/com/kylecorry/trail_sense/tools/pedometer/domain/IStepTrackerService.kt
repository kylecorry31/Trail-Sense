package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.main.persistence.ICleanable
import java.time.Instant

interface IStepTrackerService : ICleanable {
    suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod>

    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod?

    suspend fun startNewStepTrackingPeriod(endTime: Instant = Instant.now())

    suspend fun addSteps(steps: Long, time: Instant = Instant.now())

    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod)
}
