package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
                StepAddition(4, start),
                StepAddition(3, Instant.parse("2026-06-14T11:00:00Z")),
                StepAddition(3, Instant.parse("2026-06-14T12:00:00Z"))
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
                StepAddition(30, start),
                StepAddition(30, Instant.parse("2026-06-14T02:00:00Z")),
                StepAddition(30, Instant.parse("2026-06-14T03:00:00Z"))
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
                StepAddition(30, start),
                StepAddition(30, Instant.parse("2026-06-14T02:00:00Z")),
                StepAddition(30, Instant.parse("2026-06-14T03:00:00Z"))
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
            listOf(StepAddition(7, start)),
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
        val time: Instant
    )

    private class FakeStepTrackerService(
        private val openPeriod: StepTrackingPeriod? = null
    ) : IStepTrackerService {
        val addedSteps = mutableListOf<StepAddition>()

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

        override suspend fun addSteps(steps: Long, time: Instant) {
            addedSteps.add(StepAddition(steps, time))
        }

        override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) {
        }

        override suspend fun clean() {
        }
    }
}
