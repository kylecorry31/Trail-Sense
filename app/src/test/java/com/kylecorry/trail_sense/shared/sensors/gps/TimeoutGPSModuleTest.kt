package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.time.ITimer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.Duration
import java.time.Instant

class TimeoutGPSModuleTest {
    private val timer = mock<ITimer>()
    private lateinit var fireTimeout: () -> Unit
    private val data = ModularGPSData(time = Instant.EPOCH)
    private var retries = 0
    private val notifications = mutableListOf<Boolean>()
    private var retry: () -> Boolean = { false }
    private val module: TimeoutGPSModule = TimeoutGPSModule(
        tryUpdateLocation = { retries++; retry() },
        notifyListeners = { notifications.add(timedOut()) },
        logger = mock(),
        timerFactory = { fireTimeout = it; timer }
    )

    private fun timedOut() = module.isTimedOut

    @Test
    fun startSchedulesTimeoutAndStopIgnoresPendingCallback() {
        module.start(data)
        verify(timer).once(Duration.ofSeconds(10))
        module.stop(data)
        verify(timer).stop()
        fireTimeout()
        assertEquals(0, retries)
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun failedRetryMarksTimedOutBeforeNotifyingAndSchedulesAnotherAttempt() {
        module.start(data)
        fireTimeout()
        assertTrue(module.isTimedOut)
        assertEquals(listOf(true), notifications)
        verify(timer, times(2)).once(Duration.ofSeconds(10))
        fireTimeout()
        assertEquals(2, retries)
        assertEquals(listOf(true, true), notifications)
        verify(timer, times(3)).once(Duration.ofSeconds(10))
    }

    @Test
    fun acceptedUpdateClearsTimeoutAndReschedulesWhileStarted() {
        module.start(data)
        fireTimeout()
        assertTrue(module.update(data, ModularGPSData()))
        assertFalse(module.isTimedOut)
        verify(timer, times(3)).once(Duration.ofSeconds(10))
        assertEquals(1, notifications.size)
    }

    @Test
    fun updatesWhileStoppedDoNotScheduleTimer() {
        assertTrue(module.update(data, ModularGPSData()))
        assertFalse(module.isTimedOut)
        verify(timer, never()).once(Duration.ofSeconds(10))
    }

    @Test
    fun successfulRetryUsesAcceptedUpdateToResetTimer() {
        retry = { module.update(data, ModularGPSData()); true }
        module.start(data)
        fireTimeout()
        assertFalse(module.isTimedOut)
        assertEquals(listOf(false), notifications)
        verify(timer, times(2)).once(Duration.ofSeconds(10))
    }

    @Test
    fun acceptedReadingWithUnchangedTimestampStillTimesOut() {
        retry = { module.update(data, data); false }
        module.start(data)
        fireTimeout()
        assertTrue(module.isTimedOut)
        assertEquals(listOf(true), notifications)
        verify(timer, times(3)).once(Duration.ofSeconds(10))
    }

    @Test
    fun restartSchedulesTimeoutAgain() {
        module.start(data)
        module.stop(data)
        module.start(data)
        verify(timer, times(2)).once(Duration.ofSeconds(10))
        fireTimeout()
        assertEquals(1, retries)
    }
}
