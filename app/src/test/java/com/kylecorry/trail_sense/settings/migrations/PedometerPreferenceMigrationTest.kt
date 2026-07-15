package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal class PedometerPreferenceMigrationTest {

    @Test
    fun migrateDistributesStepsAcrossHourBuckets() = runBlocking {
        val start = Instant.parse("2026-06-14T10:15:00Z")
        val end = Instant.parse("2026-06-14T12:30:00Z")
        val prefs = getPrefs(start, 10)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(4, start, Duration.ofSeconds(8)),
                StepAddition(
                    3,
                    Instant.parse("2026-06-14T11:00:00Z"),
                    Duration.ofSeconds(6)
                ),
                StepAddition(
                    3,
                    Instant.parse("2026-06-14T12:00:00Z"),
                    Duration.ofSeconds(6)
                )
            ),
            stepTracker.addedSteps
        )
        verify(prefs).remove("cache_steps")
        verify(prefs).remove("last_odometer_reset")
    }

    @Test
    fun migrateDistributesStepsAcrossEveryOverlappedHourBucket() = runBlocking {
        val start = Instant.parse("2026-06-14T01:50:00Z")
        val end = Instant.parse("2026-06-14T03:30:00Z")
        val prefs = getPrefs(start, 90)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(30, start, Duration.ofSeconds(60)),
                StepAddition(
                    30,
                    Instant.parse("2026-06-14T02:00:00Z"),
                    Duration.ofSeconds(60)
                ),
                StepAddition(
                    30,
                    Instant.parse("2026-06-14T03:00:00Z"),
                    Duration.ofSeconds(60)
                )
            ),
            stepTracker.addedSteps
        )
        verify(prefs).remove("cache_steps")
        verify(prefs).remove("last_odometer_reset")
    }

    @Test
    fun migrateAddsStepsWhenOpenPeriodExists() = runBlocking {
        val start = Instant.parse("2026-06-14T01:50:00Z")
        val end = Instant.parse("2026-06-14T03:30:00Z")
        val prefs = getPrefs(start, 90)
        val stepTracker = FakeStepTrackerService(
            openPeriod = StepTrackingPeriod(
                1,
                Instant.parse("2026-06-14T02:15:00Z"),
                null,
                emptyList()
            )
        )

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(30, start, Duration.ofSeconds(60)),
                StepAddition(
                    30,
                    Instant.parse("2026-06-14T02:00:00Z"),
                    Duration.ofSeconds(60)
                ),
                StepAddition(
                    30,
                    Instant.parse("2026-06-14T03:00:00Z"),
                    Duration.ofSeconds(60)
                )
            ),
            stepTracker.addedSteps
        )
    }

    @Test
    fun migrateCreatesOneAdditionWhenEndIsBeforeStart() = runBlocking {
        val start = Instant.parse("2026-06-14T10:15:00Z")
        val end = Instant.parse("2026-06-14T10:00:00Z")
        val prefs = getPrefs(start, 7)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(StepAddition(7, start, Duration.ZERO)),
            stepTracker.addedSteps
        )
        verify(prefs).remove("cache_steps")
        verify(prefs).remove("last_odometer_reset")
    }

    @Test
    fun migrateCapsActiveTimeToBucketElapsedTime() = runBlocking {
        val start = Instant.parse("2026-06-14T10:00:00Z")
        val end = Instant.parse("2026-06-14T10:01:00Z")
        val prefs = getPrefs(start, 100)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(StepAddition(100, start, Duration.ofMinutes(1))),
            stepTracker.addedSteps
        )
    }

    @Test
    fun migrateCheckpointsCompletedAdditionsForRetry() {
        val start = Instant.parse("2026-06-14T10:15:00Z")
        val end = Instant.parse("2026-06-14T12:30:00Z")
        val prefs = getPrefs(start, 10)
        val stepTracker = FakeStepTrackerService(failOnAddition = 2)

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()
            }
        }

        assertEquals(listOf(StepAddition(4, start, Duration.ofSeconds(8))), stepTracker.addedSteps)
        verify(prefs).putLong("cache_steps", 6)
        verify(prefs).putInstant(
            "last_odometer_reset",
            Instant.parse("2026-06-14T11:00:00Z")
        )
        verify(prefs, never()).remove("cache_steps")
        verify(prefs, never()).remove("last_odometer_reset")

        // Retry migration
        whenever(prefs.getLong("cache_steps")).thenReturn(6)
        whenever(prefs.getInstant("last_odometer_reset")).thenReturn(
            Instant.parse("2026-06-14T11:00:00Z")
        )

        runBlocking {
            PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()
        }

        assertEquals(
            listOf(
                StepAddition(4, start, Duration.ofSeconds(8)),
                StepAddition(
                    3,
                    Instant.parse("2026-06-14T11:00:00Z"),
                    Duration.ofSeconds(6)
                ),
                StepAddition(
                    3,
                    Instant.parse("2026-06-14T12:00:00Z"),
                    Duration.ofSeconds(6)
                )
            ),
            stepTracker.addedSteps
        )
        verify(prefs).remove("cache_steps")
        verify(prefs).remove("last_odometer_reset")
    }

    private fun getPrefs(startTime: Instant, steps: Long): IPreferences {
        val prefs = mock<IPreferences>()
        whenever(prefs.getLong("cache_steps")).thenReturn(steps)
        whenever(prefs.getInstant("last_odometer_reset")).thenReturn(startTime)
        return prefs
    }

    private data class StepAddition(
        val steps: Long,
        val time: Instant,
        val activeTime: Duration
    )

    private class FakeStepTrackerService(
        private val openPeriod: StepTrackingPeriod? = null,
        private val failOnAddition: Int? = null
    ) : IStepTrackerService {
        val addedSteps = mutableListOf<StepAddition>()
        private var additionCount = 0

        override suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> {
            return emptyList()
        }

        override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
            return openPeriod
        }

        override suspend fun getHourlyStepCounts(
            date: LocalDate,
            zoneId: ZoneId
        ): List<HourlyStepCount> {
            return emptyList()
        }

        override suspend fun startNewStepTrackingPeriod(endTime: Instant) {
        }

        override suspend fun addSteps(steps: Long, time: Instant, activeTime: Duration) {
            additionCount++
            if (additionCount == failOnAddition) {
                throw IllegalStateException("Failed to add steps")
            }
            addedSteps.add(StepAddition(steps, time, activeTime))
        }

        override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) {
        }

        override suspend fun clean() {
        }
    }
}
