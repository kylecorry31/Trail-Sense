package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.main.persistence.ICleanable
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface IStepTrackerService : ICleanable {
    suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod>

    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod?

    suspend fun getHourlyStepCounts(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<HourlyStepCount>

    suspend fun startNewStepTrackingPeriod(endTime: Instant = Instant.now())

    suspend fun addSteps(
        steps: Long,
        time: Instant = Instant.now(),
        activeTime: Duration = Duration.ZERO
    )

    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod)
}
