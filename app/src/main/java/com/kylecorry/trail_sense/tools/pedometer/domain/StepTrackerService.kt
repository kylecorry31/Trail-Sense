package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class StepTrackerService(private val repository: IStepTrackerRepository) {

    private val stepMutex = Mutex()

    suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> {
        return repository.getStepTrackingPeriods()
    }

    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
        return repository.getOpenStepTrackingPeriod()
    }

    suspend fun closeStepTrackingPeriod(endTime: Instant = Instant.now()) = stepMutex.withLock {
        val openPeriod = repository.getOpenStepTrackingPeriod() ?: return@withLock
        val closedPeriod = openPeriod.copy(endTime = endTime)
        repository.upsertStepTrackingPeriod(closedPeriod)
        // TODO: Emit event
    }

    suspend fun addSteps(steps: Long, time: Instant = Instant.now()) = stepMutex.withLock {
        val openPeriod = getOrCreateOpenStepTrackingPeriodWithoutLock(time)
        val containedBucket = openPeriod.stepCountBuckets.firstOrNull {
            (time == it.startTime || time.isAfter(it.startTime)) && time.isBefore(it.endTime)
        }
        val bucketToAdd = if (containedBucket != null) {
            containedBucket.copy(steps = containedBucket.steps + steps)
        } else {
            // Buckets always start on the hour and are 1 hour long
            val startTime = time.truncatedTo(ChronoUnit.HOURS)
            val endTime = startTime.plus(DEFAULT_BUCKET_DURATION)
            StepCountBucket(
                id = 0,
                periodId = openPeriod.id,
                startTime = startTime,
                endTime = endTime,
                steps = steps
            )
        }
        repository.addStepCountBucket(bucketToAdd)
        // TODO: Emit event
    }

    private suspend fun getOrCreateOpenStepTrackingPeriodWithoutLock(startTime: Instant): StepTrackingPeriod {
        val openPeriod = repository.getOpenStepTrackingPeriod()
        if (openPeriod != null) {
            return openPeriod
        }

        val newPeriod = StepTrackingPeriod(
            id = 0,
            startTime = startTime,
            endTime = null,
            stepCountBuckets = emptyList()
        )
        val newId = repository.upsertStepTrackingPeriod(newPeriod)
        return newPeriod.copy(id = newId)
    }

    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) = stepMutex.withLock {
        repository.deleteBucketsInPeriod(period.id)
        repository.deleteStepTrackingPeriod(period)
        // TODO: Emit event
    }

    companion object {
        private val DEFAULT_BUCKET_DURATION = Duration.ofHours(1)
    }
}
