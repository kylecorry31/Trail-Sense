package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.only
import org.mockito.kotlin.verify
import java.time.Instant

internal class StepTrackerServiceTest {

    private lateinit var repository: FakeStepTrackerRepository
    private lateinit var eventBus: IEventEmitter
    private lateinit var service: StepTrackerService

    @BeforeEach
    fun setup() {
        repository = FakeStepTrackerRepository()
        eventBus = mock()
        service = StepTrackerService(repository, eventBus)
    }

    @Test
    fun getAllStepTrackingPeriodsReturnsRepositoryPeriodsWithBuckets() = runBlocking {
        val startTime = Instant.parse("2026-01-01T00:00:00Z")
        val period = repository.addPeriod(period(id = 1, startTime = startTime))
        val bucket = bucket(id = 2, periodId = period.id, startTime = startTime, steps = 100)
        repository.addBucket(bucket)

        val periods = service.getAllStepTrackingPeriods()

        assertEquals(listOf(period.copy(stepCountBuckets = listOf(bucket))), periods)
    }

    @Test
    fun getAllStepTrackingPeriodsReturnsEmptyListWhenRepositoryIsEmpty() = runBlocking {
        val periods = service.getAllStepTrackingPeriods()
        assertEquals(emptyList<StepTrackingPeriod>(), periods)
    }

    @Test
    fun startNewStepTrackingPeriodClosesOpenPeriodWithStepsAndCreatesNewOpenPeriod() = runBlocking {
        val startTime = Instant.parse("2026-01-01T10:15:00Z")
        val endTime = Instant.parse("2026-01-01T12:00:00Z")
        val openPeriod = repository.addPeriod(period(id = 1, startTime = startTime))
        repository.addBucket(bucket(id = 2, periodId = openPeriod.id, startTime = startTime, steps = 20))

        service.startNewStepTrackingPeriod(endTime)

        assertEquals(endTime, repository.periods.first { it.id == openPeriod.id }.endTime)

        val newOpenPeriod = service.getOpenStepTrackingPeriod()
        assertEquals(endTime, newOpenPeriod?.startTime)
        assertEquals(emptyList<StepCountBucket>(), newOpenPeriod?.stepCountBuckets)
        verifyStepsChanged(0)
    }

    @Test
    fun startNewStepTrackingPeriodDeletesEmptyOpenPeriodBeforeCreatingNewPeriod() = runBlocking {
        val startTime = Instant.parse("2026-01-01T10:15:00Z")
        val endTime = Instant.parse("2026-01-01T12:00:00Z")
        val emptyOpenPeriod = repository.addPeriod(period(id = 1, startTime = startTime))

        service.startNewStepTrackingPeriod(endTime)

        assertEquals(listOf(emptyOpenPeriod.id), repository.deletedBucketPeriodIds)
        assertEquals(listOf(emptyOpenPeriod), repository.deletedPeriods)
        assertEquals(true, repository.periods.none { it.id == emptyOpenPeriod.id })
        assertEquals(endTime, service.getOpenStepTrackingPeriod()?.startTime)
        verifyStepsChanged(0)
    }

    @Test
    fun addStepsCreatesOpenPeriodAndHourlyBucketWhenNoPeriodExists() = runBlocking {
        val time = Instant.parse("2026-01-01T10:15:30Z")
        service.addSteps(12, time)

        val openPeriod = service.getOpenStepTrackingPeriod()
        assertEquals(time, openPeriod?.startTime)
        assertEquals(12, openPeriod?.steps)
        assertEquals(
            listOf(
                bucket(
                    id = 2,
                    periodId = 1,
                    startTime = Instant.parse("2026-01-01T10:00:00Z"),
                    endTime = Instant.parse("2026-01-01T11:00:00Z"),
                    steps = 12
                )
            ),
            openPeriod?.stepCountBuckets
        )
        verifyStepsChanged(12)
    }

    @Test
    fun addStepsAddsToExistingBucketWhenTimeIsInsideBucket() = runBlocking {
        val startTime = Instant.parse("2026-01-01T10:00:00Z")
        val openPeriod = repository.addPeriod(period(id = 1, startTime = startTime))
        repository.addBucket(bucket(id = 2, periodId = openPeriod.id, startTime = startTime, steps = 10))

        service.addSteps(7, Instant.parse("2026-01-01T10:59:59Z"))

        assertEquals(17, service.getOpenStepTrackingPeriod()?.steps)
        assertEquals(1, repository.buckets.size)
        assertEquals(17, repository.buckets.first().steps)
        verifyStepsChanged(17)
    }

    @Test
    fun addStepsCreatesNewBucketWhenTimeIsAtExistingBucketEnd() = runBlocking {
        val startTime = Instant.parse("2026-01-01T10:00:00Z")
        val openPeriod = repository.addPeriod(period(id = 1, startTime = startTime))
        repository.addBucket(bucket(id = 2, periodId = openPeriod.id, startTime = startTime, steps = 10))

        service.addSteps(7, Instant.parse("2026-01-01T11:00:00Z"))

        val buckets = service.getOpenStepTrackingPeriod()?.stepCountBuckets.orEmpty()
        assertEquals(2, buckets.size)
        assertEquals(17, buckets.sumOf { it.steps })
        assertEquals(
            bucket(
                id = 3,
                periodId = openPeriod.id,
                startTime = Instant.parse("2026-01-01T11:00:00Z"),
                endTime = Instant.parse("2026-01-01T12:00:00Z"),
                steps = 7
            ),
            buckets.last()
        )
        verifyStepsChanged(17)
    }

    @Test
    fun deleteStepTrackingPeriodDeletesBucketsAndPeriod() = runBlocking {
        val period = repository.addPeriod(period(id = 1))
        repository.addBucket(bucket(id = 2, periodId = period.id))

        service.deleteStepTrackingPeriod(period)

        assertEquals(listOf(period.id), repository.deletedBucketPeriodIds)
        assertEquals(listOf(period), repository.deletedPeriods)
        assertEquals(emptyList<StepTrackingPeriod>(), repository.periods)
        assertEquals(emptyList<StepCountBucket>(), repository.buckets)
        verifyStepsChanged(0)
    }

    private fun verifyStepsChanged(steps: Long) {
        verify(eventBus, only()).broadcast(
            eq(PedometerToolRegistration.BROADCAST_STEPS_CHANGED),
            argThat {
                getLong(PedometerToolRegistration.BROADCAST_PARAM_STEPS) == steps
            }
        )
    }

    private fun period(
        id: Long,
        startTime: Instant = Instant.parse("2026-01-01T10:00:00Z"),
        endTime: Instant? = null,
        buckets: List<StepCountBucket> = emptyList()
    ): StepTrackingPeriod {
        return StepTrackingPeriod(id, startTime, endTime, buckets)
    }

    private fun bucket(
        id: Long,
        periodId: Long,
        startTime: Instant = Instant.parse("2026-01-01T10:00:00Z"),
        endTime: Instant = startTime.plusSeconds(3600),
        steps: Long = 1
    ): StepCountBucket {
        return StepCountBucket(id, periodId, startTime, endTime, steps)
    }

    private class FakeStepTrackerRepository : IStepTrackerRepository {
        val periods = mutableListOf<StepTrackingPeriod>()
        val buckets = mutableListOf<StepCountBucket>()
        val deletedPeriods = mutableListOf<StepTrackingPeriod>()
        val deletedBucketPeriodIds = mutableListOf<Long>()

        private var nextId = 1L

        override suspend fun getStepTrackingPeriods(): List<StepTrackingPeriod> {
            return periods.map { it.withBuckets() }
        }

        override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
            return periods.firstOrNull { it.isOpen }?.withBuckets()
        }

        override suspend fun upsertStepTrackingPeriod(period: StepTrackingPeriod): Long {
            val periodToSave = if (period.id == 0L) {
                period.copy(id = nextId++)
            } else {
                period
            }

            periods.removeAll { it.id == periodToSave.id }
            periods.add(periodToSave.copy(stepCountBuckets = emptyList()))
            nextId = maxOf(nextId, periodToSave.id + 1)
            return periodToSave.id
        }

        override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) {
            periods.removeAll { it.id == period.id }
            deletedPeriods.add(period)
        }

        override suspend fun upsertStepCountBucket(bucket: StepCountBucket): Long {
            val bucketToSave = if (bucket.id == 0L) {
                bucket.copy(id = nextId++)
            } else {
                bucket
            }

            buckets.removeAll { it.id == bucketToSave.id }
            buckets.add(bucketToSave)
            nextId = maxOf(nextId, bucketToSave.id + 1)
            return bucketToSave.id
        }

        override suspend fun deleteBucketsInPeriod(periodId: Long) {
            buckets.removeAll { it.periodId == periodId }
            deletedBucketPeriodIds.add(periodId)
        }

        fun addPeriod(period: StepTrackingPeriod): StepTrackingPeriod {
            periods.add(period.copy(stepCountBuckets = emptyList()))
            nextId = maxOf(nextId, period.id + 1)
            return period
        }

        fun addBucket(bucket: StepCountBucket): StepCountBucket {
            buckets.add(bucket)
            nextId = maxOf(nextId, bucket.id + 1)
            return bucket
        }

        private fun StepTrackingPeriod.withBuckets(): StepTrackingPeriod {
            return copy(stepCountBuckets = buckets.filter { it.periodId == id })
        }
    }

}
