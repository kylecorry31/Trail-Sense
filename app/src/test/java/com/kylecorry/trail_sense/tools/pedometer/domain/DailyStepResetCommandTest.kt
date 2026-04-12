package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IStepCounter
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
    private lateinit var isToday: Specification<Instant>
    private lateinit var counter: IStepCounter

    @BeforeEach
    fun setup(){
        counter = mock()
        isToday = mock()

        command = DailyStepResetCommand(counter, isToday = isToday)
    }

    @Test
    fun resetsWhenNoResetToday() {
        val time = Instant.ofEpochMilli(100)
        whenever(counter.startTime).thenReturn(time)
        whenever(isToday.isSatisfiedBy(time)).thenReturn(false)

        command.execute()

        verify(counter).reset()
    }

    @Test
    fun noResetWhenAlreadyResetToday() {
        val time = Instant.ofEpochMilli(100)
        whenever(counter.startTime).thenReturn(time)
        whenever(isToday.isSatisfiedBy(time)).thenReturn(true)

        command.execute()

        verify(counter, never()).reset()
    }

    @Test
    fun resetsWhenNeverReset() {
        whenever(counter.startTime).thenReturn(null)

        command.execute()

        verify(counter).reset()
        verify(isToday, never()).isSatisfiedBy(any())
    }
}
