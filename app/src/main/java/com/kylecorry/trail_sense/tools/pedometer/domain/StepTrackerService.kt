package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class StepTrackerService(private val repository: IStepTrackerRepository, private val eventBus: IEventEmitter) :
    IStepTrackerService {

    private val stepMutex = Mutex()

    override suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> {
        return repository.getStepTrackingPeriods()
    }

    override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
        return repository.getOpenStepTrackingPeriod()
    }

    override suspend fun startNewStepTrackingPeriod(endTime: Instant) = stepMutex.withLock {
        closeStepTrackingPeriodWithoutLock(endTime)
        val openPeriod = getOrCreateOpenStepTrackingPeriodWithoutLock(endTime)
        emitStepsChanged(openPeriod.steps)
    }

    private suspend fun closeStepTrackingPeriodWithoutLock(endTime: Instant) {
        val openPeriod = repository.getOpenStepTrackingPeriod() ?: return
        if (openPeriod.steps == 0L) {
            repository.deleteBucketsInPeriod(openPeriod.id)
            repository.deleteStepTrackingPeriod(openPeriod)
        } else {
            repository.upsertStepTrackingPeriod(openPeriod.copy(endTime = endTime))
        }
    }

    override suspend fun addSteps(steps: Long, time: Instant) = stepMutex.withLock {
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
        repository.upsertStepCountBucket(bucketToAdd)
        emitStepsChanged(openPeriod.steps + steps)
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

    override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) = stepMutex.withLock {
        repository.deleteBucketsInPeriod(period.id)
        repository.deleteStepTrackingPeriod(period)
        emitStepsChanged(repository.getOpenStepTrackingPeriod()?.steps ?: 0)
    }

    private fun emitStepsChanged(steps: Long) {
        eventBus.broadcast(
            PedometerToolRegistration.BROADCAST_STEPS_CHANGED,
            EventData().apply {
                putLong(PedometerToolRegistration.BROADCAST_PARAM_STEPS, steps)
            }
        )
    }

    companion object {
        private val DEFAULT_BUCKET_DURATION = Duration.ofHours(1)
    }
}
