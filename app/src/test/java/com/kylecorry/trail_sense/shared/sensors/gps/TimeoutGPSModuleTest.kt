package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.luna.time.ITimer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.Instant

class TimeoutGPSModuleTest {
    private val timer = mock<ITimer>()
    private lateinit var fireTimeout: () -> Unit
    private val data = ModularGPSData(time = Instant.EPOCH)
    private val notifications = mutableListOf<Boolean>()
    private val module: TimeoutGPSModule = TimeoutGPSModule(
        notifyListeners = { notifications.add(timedOut()) },
        logger = mock(),
        timerFactory = { fireTimeout = it; timer }
    )

    private fun timedOut() = data.isTimedOut

    private fun accept(candidate: ModularGPSData): Boolean {
        val accepted = module.update(data, candidate)
        if (accepted) {
            candidate.copyInto(data)
        }
        return accepted
    }

    @Test
    fun startSchedulesTimeoutAndStopIgnoresPendingCallback() {
        module.start(data)
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)
        module.stop(data)
        verify(timer).stop()
        fireTimeout()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun marksTimedOutBeforeNotifyingWithoutRearming() {
        module.start(data)
        fireTimeout()
        assertTrue(data.isTimedOut)
        assertEquals(listOf(true), notifications)
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)
    }

    @Test
    fun acceptedUpdateClearsTimeoutAndReschedulesWhileStarted() {
        module.start(data)
        fireTimeout()
        assertTrue(accept(ModularGPSData()))
        assertFalse(data.isTimedOut)
        verify(timer, times(2)).once(SensorService.GPS_READ_TIMEOUT)
        assertEquals(1, notifications.size)
    }

    @Test
    fun updatesWhileStoppedDoNotScheduleTimer() {
        assertTrue(accept(ModularGPSData()))
        assertFalse(data.isTimedOut)
        verify(timer, never()).once(SensorService.GPS_READ_TIMEOUT)
    }

    @Test
    fun secondaryUpdatesDoNotPostponeTimeoutOrClearTimedOutState() {
        module.start(data)
        val duplicate = ModularGPSData(time = data.time.plusNanos(123456), satellites = 6)
        repeat(20) {
            assertTrue(accept(duplicate))
        }
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)

        fireTimeout()
        assertTrue(data.isTimedOut)
        assertTrue(accept(duplicate))
        assertTrue(data.isTimedOut)
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)

        accept(ModularGPSData(time = data.time.plusSeconds(1)))
        assertFalse(data.isTimedOut)
        verify(timer, times(2)).once(SensorService.GPS_READ_TIMEOUT)
    }

    @Test
    fun restartSchedulesTimeoutAgain() {
        module.start(data)
        module.stop(data)
        module.start(data)
        verify(timer, times(2)).once(SensorService.GPS_READ_TIMEOUT)
        fireTimeout()
        assertTrue(data.isTimedOut)
    }
}
