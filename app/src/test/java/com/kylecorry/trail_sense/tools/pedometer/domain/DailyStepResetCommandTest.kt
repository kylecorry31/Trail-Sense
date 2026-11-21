package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.luna.specifications.Specification
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IPedometerPreferences
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

internal class DailyStepResetCommandTest {

    private lateinit var command: DailyStepResetCommand
    private lateinit var prefs: IPedometerPreferences
    private lateinit var isToday: Specification<Instant>
    private lateinit var stepTrackerService: IStepTrackerService

    @BeforeEach
    fun setup() {
        prefs = mock()
        stepTrackerService = mock()
        isToday = mock()

        command = DailyStepResetCommand(prefs, stepTrackerService, isToday)
    }

    @Test
    fun resetsWhenEnabledAndNoResetToday() = runBlocking {
        val time = Instant.ofEpochMilli(100)
        whenever(prefs.resetDaily).thenReturn(true)
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            StepTrackingPeriod(
                0,
                time,
                null,
                emptyList()
            )
        )
        whenever(isToday.isSatisfiedBy(time)).thenReturn(false)

        command.execute()

        verify(stepTrackerService).startNewStepTrackingPeriod(any())
    }

    @Test
    fun noResetWhenEnabledAndAlreadyResetToday() = runBlocking {
        val time = Instant.ofEpochMilli(100)
        whenever(prefs.resetDaily).thenReturn(true)
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(
            StepTrackingPeriod(
                0,
                time,
                null,
                emptyList()
            )
        )
        whenever(isToday.isSatisfiedBy(time)).thenReturn(true)

        command.execute()

        verify(stepTrackerService, never()).startNewStepTrackingPeriod(any())
    }

    @Test
    fun noResetWhenDisabled() = runBlocking {
        whenever(prefs.resetDaily).thenReturn(false)
        whenever(isToday.isSatisfiedBy(any())).thenReturn(true)

        command.execute()

        verify(stepTrackerService, never()).startNewStepTrackingPeriod(any())
    }

    @Test
    fun resetsWhenEnabledAndNeverReset(): Unit = runBlocking {
        whenever(prefs.resetDaily).thenReturn(true)
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(null)

        command.execute()

        verify(stepTrackerService).startNewStepTrackingPeriod(any())
        verify(isToday, never()).isSatisfiedBy(any())
    }
}
