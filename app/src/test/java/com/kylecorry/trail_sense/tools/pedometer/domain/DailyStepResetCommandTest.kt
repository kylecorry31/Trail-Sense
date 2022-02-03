package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IStepCounter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

internal class DailyStepResetCommandTest {

    private lateinit var command: DailyStepResetCommand
    private lateinit var prefs: IPedometerPreferences
    private lateinit var isToday: Specification<Instant>
    private lateinit var counter: IStepCounter

    @BeforeEach
    fun setup(){
        prefs = mock()
        counter = mock()
        isToday = mock()

        command = DailyStepResetCommand(prefs, counter, isToday)
    }

    @Test
    fun resetsWhenEnabledAndNoResetToday() {
        // Arrange
        val time = Instant.ofEpochMilli(100)
        whenever(prefs.resetDaily).thenReturn(true)
        whenever(counter.startTime).thenReturn(time)
        whenever(isToday.isSatisfiedBy(time)).thenReturn(false)

        // Act
        command.execute()

        // Assert
        verify(counter).reset()
    }

    @Test
    fun noResetWhenEnabledAndAlreadyResetToday() {
        // Arrange
        val time = Instant.ofEpochMilli(100)
        whenever(prefs.resetDaily).thenReturn(true)
        whenever(counter.startTime).thenReturn(time)
        whenever(isToday.isSatisfiedBy(time)).thenReturn(true)

        // Act
        command.execute()

        // Assert
        verify(counter, never()).reset()
    }

    @Test
    fun noResetWhenDisabled() {
        // Arrange
        whenever(prefs.resetDaily).thenReturn(false)
        whenever(isToday.isSatisfiedBy(any())).thenReturn(true)

        // Act
        command.execute()

        // Assert
        verify(counter, never()).reset()
    }

    @Test
    fun resetsWhenEnabledAndNeverReset() {
        // Arrange
        whenever(prefs.resetDaily).thenReturn(true)
        whenever(counter.startTime).thenReturn(null)

        // Act
        command.execute()

        // Assert
        verify(counter).reset()
        verify(isToday, never()).isSatisfiedBy(any())
    }
}