package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

class StepTrackerService(
    private val repository: IStepTrackerRepository,
    private val eventBus: IEventEmitter,
    private val preferences: IPedometerPreferences,
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) :
    IStepTrackerService {

    private val stepMutex = Mutex()

    override suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> {
        return repository.getStepTrackingPeriods()
    }

    override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
        return repository.getOpenStepTrackingPeriod()
    }

    override suspend fun getHourlyStepCounts(
        date: LocalDate,
        zoneId: ZoneId
    ): List<HourlyStepCount> {
        val startTime = date.atStartOfDay(zoneId).toInstant()
        val endTime = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val buckets = repository.getStepCountBuckets(startTime, endTime)

        val hourlyStepCounts = mutableListOf<HourlyStepCount>()
        var hourStart = startTime
        while (hourStart.isBefore(endTime)) {
            val hourEnd = hourStart.plus(Duration.ofHours(1))
            val steps = buckets.sumOf { bucket ->
                getProportionalSteps(bucket, hourStart, hourEnd).toDouble()
            }.roundToLong()
            hourlyStepCounts.add(HourlyStepCount(hourStart, hourEnd, steps))
            hourStart = hourEnd
        }
        return hourlyStepCounts
    }

    private fun getProportionalSteps(
        bucket: StepCountBucket,
        startTime: Instant,
        endTime: Instant
    ): Float {
        val bucketDuration = Duration.between(bucket.startTime, bucket.endTime).toMillis()
        val overlapStart = max(bucket.startTime.toEpochMilli(), startTime.toEpochMilli())
        val overlapEnd = min(bucket.endTime.toEpochMilli(), endTime.toEpochMilli())
        val overlapDuration = overlapEnd - overlapStart
        return if (bucketDuration > 0 && overlapDuration > 0) {
            bucket.steps * overlapDuration / bucketDuration.toFloat()
        } else {
            0f
        }
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

    override suspend fun addSteps(
        steps: Long,
        time: Instant,
        activeTime: Duration
    ) = stepMutex.withLock {
        val existingOpenPeriod = getOrCreateOpenStepTrackingPeriodWithoutLock(time)
        val openPeriod = if (time.isBefore(existingOpenPeriod.startTime)) {
            existingOpenPeriod.copy(startTime = time).also {
                repository.upsertStepTrackingPeriod(it)
            }
        } else {
            existingOpenPeriod
        }
        val containedBucket = openPeriod.stepCountBuckets.firstOrNull {
            (time == it.startTime || time.isAfter(it.startTime)) && time.isBefore(it.endTime)
        }
        val bucketToAdd = if (containedBucket != null) {
            containedBucket.copy(
                steps = containedBucket.steps + steps,
                activeTime = containedBucket.activeTime.plus(activeTime)
            )
        } else {
            // Buckets always start on the hour and are 1 hour long
            val startTime = time.toZonedDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .toInstant()
            val endTime = startTime.toZonedDateTime()
                .plus(DEFAULT_BUCKET_DURATION)
                .toInstant()
            StepCountBucket(
                id = 0,
                periodId = openPeriod.id,
                startTime = startTime,
                endTime = endTime,
                steps = steps,
                activeTime = activeTime
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

    override suspend fun clean() = stepMutex.withLock {
        val cutoff = timeProvider.getTime().toInstant().minus(preferences.stepHistory)
        val deletedBuckets = repository.deleteBucketsOlderThan(cutoff)
        val deletedPeriods = repository.deleteEmptyClosedPeriods()
        val updatedPeriods = repository.setMinimumPeriodStartTime(cutoff)
        if (deletedBuckets || deletedPeriods || updatedPeriods) {
            emitStepsChanged(repository.getOpenStepTrackingPeriod()?.steps ?: 0)
        }
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
