package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal class StepTrackerServiceTest {

    private lateinit var repository: FakeStepTrackerRepository
    private lateinit var eventBus: IEventEmitter
    private lateinit var preferences: IPedometerPreferences
    private lateinit var timeProvider: ITimeProvider
    private lateinit var service: StepTrackerService

    @BeforeEach
    fun setup() {
        repository = FakeStepTrackerRepository()
        eventBus = mock()
        preferences = mock()
        timeProvider = mock()
        whenever(preferences.stepHistory).thenReturn(Duration.ofDays(30))
        whenever(timeProvider.getTime()).thenReturn(ZonedDateTime.ofInstant(NOW, ZoneId.of("UTC")))
        service = StepTrackerService(repository, eventBus, preferences, timeProvider)
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
    fun getHourlyStepCountsSplitsBucketsProportionallyAcrossHours() = runBlocking {
        val period = repository.addPeriod(
            period(
                id = 1,
                startTime = Instant.parse("2026-01-01T23:30:00Z"),
                endTime = Instant.parse("2026-01-02T02:30:00Z")
            )
        )
        repository.addBucket(
            bucket(
                id = 2,
                periodId = period.id,
                startTime = Instant.parse("2026-01-01T23:30:00Z"),
                endTime = Instant.parse("2026-01-02T01:30:00Z"),
                steps = 120
            )
        )
        repository.addBucket(
            bucket(
                id = 3,
                periodId = period.id,
                startTime = Instant.parse("2026-01-02T01:00:00Z"),
                endTime = Instant.parse("2026-01-02T02:00:00Z"),
                steps = 60
            )
        )

        val hourlySteps = service.getHourlyStepCounts(
            LocalDate.of(2026, 1, 2),
            ZoneId.of("UTC")
        )

        assertEquals(60L, hourlySteps[0].steps)
        assertEquals(90L, hourlySteps[1].steps)
        assertEquals(0L, hourlySteps[2].steps)
        assertEquals(150L, hourlySteps.sumOf { it.steps })
    }

    @Test
    fun getHourlyStepCountsUsesActualHoursOnSpringDstDay() = runBlocking {
        val zone = ZoneId.of("America/New_York")
        val date = LocalDate.of(2026, 3, 8)
        val startTime = date.atStartOfDay(zone).toInstant()
        val endTime = date.plusDays(1).atStartOfDay(zone).toInstant()
        val period = repository.addPeriod(period(id = 1, startTime = startTime, endTime = endTime))
        repository.addBucket(
            bucket(
                id = 2,
                periodId = period.id,
                startTime = startTime,
                endTime = endTime,
                steps = 230
            )
        )

        val hourlySteps = service.getHourlyStepCounts(date, zone)

        assertEquals(23, hourlySteps.size)
        assertEquals(startTime, hourlySteps.first().startTime)
        assertEquals(endTime, hourlySteps.last().endTime)
        assertEquals(230L, hourlySteps.sumOf { it.steps })
    }

    @Test
    fun getHourlyStepCountsUsesActualHoursOnFallDstDay() = runBlocking {
        val zone = ZoneId.of("America/New_York")
        val date = LocalDate.of(2026, 11, 1)
        val startTime = date.atStartOfDay(zone).toInstant()
        val endTime = date.plusDays(1).atStartOfDay(zone).toInstant()
        val period = repository.addPeriod(period(id = 1, startTime = startTime, endTime = endTime))
        repository.addBucket(
            bucket(
                id = 2,
                periodId = period.id,
                startTime = startTime,
                endTime = endTime,
                steps = 250
            )
        )

        val hourlySteps = service.getHourlyStepCounts(date, zone)

        assertEquals(25, hourlySteps.size)
        assertEquals(startTime, hourlySteps.first().startTime)
        assertEquals(endTime, hourlySteps.last().endTime)
        assertEquals(250L, hourlySteps.sumOf { it.steps })
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
    fun addStepsMovesOpenPeriodStartTimeEarlierWhenTimeIsBeforeStartTime() = runBlocking {
        val startTime = Instant.parse("2026-01-01T10:00:00Z")
        val earlierTime = Instant.parse("2026-01-01T09:15:00Z")
        val openPeriod = repository.addPeriod(period(id = 1, startTime = startTime))
        repository.addBucket(bucket(id = 2, periodId = openPeriod.id, startTime = startTime, steps = 10))

        service.addSteps(7, earlierTime)

        val updatedPeriod = service.getOpenStepTrackingPeriod()
        val buckets = updatedPeriod?.stepCountBuckets.orEmpty().sortedBy { it.startTime }
        assertEquals(earlierTime, updatedPeriod?.startTime)
        assertEquals(17, updatedPeriod?.steps)
        assertEquals(
            bucket(
                id = 3,
                periodId = openPeriod.id,
                startTime = Instant.parse("2026-01-01T09:00:00Z"),
                endTime = Instant.parse("2026-01-01T10:00:00Z"),
                steps = 7
            ),
            buckets.first()
        )
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

    @Test
    fun cleanDeletesBucketsOlderThanStepHistory() = runBlocking {
        val period = repository.addPeriod(period(id = 1, endTime = NOW))
        val oldBucket = bucket(
            id = 2,
            periodId = period.id,
            startTime = NOW.minus(Duration.ofDays(31)),
            endTime = NOW.minus(Duration.ofDays(30)).minusSeconds(1)
        )
        val recentBucket = bucket(
            id = 3,
            periodId = period.id,
            startTime = NOW.minus(Duration.ofDays(30)),
            endTime = NOW.minus(Duration.ofDays(30))
        )
        repository.addBucket(oldBucket)
        repository.addBucket(recentBucket)

        service.clean()

        assertEquals(listOf(recentBucket), repository.buckets)
        verifyNoInteractions(eventBus)
    }

    @Test
    fun cleanDeletesEmptyClosedPeriods() = runBlocking {
        val emptyClosedPeriod = repository.addPeriod(period(id = 1, endTime = NOW))
        val emptyOpenPeriod = repository.addPeriod(period(id = 2, endTime = null))
        val nonEmptyClosedPeriod = repository.addPeriod(period(id = 3, endTime = NOW))
        repository.addBucket(bucket(id = 4, periodId = nonEmptyClosedPeriod.id, endTime = NOW))

        service.clean()

        assertEquals(listOf(emptyClosedPeriod), repository.deletedPeriods)
        assertEquals(listOf(emptyOpenPeriod, nonEmptyClosedPeriod), repository.periods)
        verifyNoInteractions(eventBus)
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

        override suspend fun getStepCountBuckets(
            startTime: Instant,
            endTime: Instant
        ): List<StepCountBucket> {
            return buckets.filter { it.startTime < endTime && it.endTime > startTime }
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

        override suspend fun deleteBucketsOlderThan(endTime: Instant) {
            buckets.removeAll { it.endTime.isBefore(endTime) }
        }

        override suspend fun deleteEmptyClosedPeriods() {
            val emptyClosedPeriods = periods.filter { period ->
                period.endTime != null && buckets.none { it.periodId == period.id }
            }
            periods.removeAll(emptyClosedPeriods)
            deletedPeriods.addAll(emptyClosedPeriods)
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

    companion object {
        private val NOW = Instant.parse("2026-06-14T12:00:00Z")
    }

}
