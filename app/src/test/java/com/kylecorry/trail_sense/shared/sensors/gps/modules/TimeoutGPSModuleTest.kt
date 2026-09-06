package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.luna.time.ITimer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class TimeoutGPSModuleTest {
    private val timer = mock<ITimer>()
    private lateinit var fireTimeout: suspend () -> Unit
    private val data = ModularGPSData(time = Instant.EPOCH)
    private val notifications = mutableListOf<Boolean>()
    private val module: TimeoutGPSModule = TimeoutGPSModule(
        onTimeout = { acceptTimeout ->
            if (acceptTimeout()) {
                data.isTimedOut = true
                notifications.add(timedOut())
            }
        },
        logger = mock(),
        timerFactory = { fireTimeout = it; timer }
    )

    private fun timedOut() = data.isTimedOut

    private suspend fun accept(candidate: ModularGPSData): Boolean {
        val accepted = module.update(data, candidate)
        if (accepted) {
            candidate.copyInto(data)
        }
        return accepted
    }

    @Test
    fun startSchedulesTimeoutAndStopIgnoresPendingCallback() = runBlocking<Unit> {
        module.start(data)
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)
        module.stop(data)
        verify(timer).stop()
        fireTimeout()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun timeoutLeavesStateChangeToTheCallback() = runBlocking<Unit> {
        var accepted = false
        val timeoutModule = TimeoutGPSModule(
            onTimeout = { acceptTimeout -> accepted = acceptTimeout() },
            logger = mock(),
            timerFactory = { fireTimeout = it; timer }
        )
        timeoutModule.start(data)
        fireTimeout()
        assertTrue(accepted)
        assertFalse(data.isTimedOut)
        timeoutModule.stop(data)
    }

    @Test
    fun marksTimedOutBeforeNotifyingWithoutRearming() = runBlocking<Unit> {
        module.start(data)
        fireTimeout()
        assertTrue(data.isTimedOut)
        assertEquals(listOf(true), notifications)
        verify(timer).once(SensorService.GPS_READ_TIMEOUT)
    }

    @Test
    fun acceptedUpdateClearsTimeoutAndReschedulesWhileStarted() = runBlocking<Unit> {
        module.start(data)
        fireTimeout()
        assertTrue(accept(ModularGPSData()))
        assertFalse(data.isTimedOut)
        verify(timer, times(2)).once(SensorService.GPS_READ_TIMEOUT)
        assertEquals(1, notifications.size)
    }

    @Test
    fun updatesWhileStoppedDoNotScheduleTimer() = runBlocking<Unit> {
        assertTrue(accept(ModularGPSData()))
        assertFalse(data.isTimedOut)
        verify(timer, never()).once(SensorService.GPS_READ_TIMEOUT)
    }

    @Test
    fun secondaryUpdatesDoNotPostponeTimeoutOrClearTimedOutState() = runBlocking<Unit> {
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
    fun callbacksFromBeforeRestartCannotTimeoutTheNewSession() = runBlocking<Unit> {
        module.start(data)
        val oldTimeout = fireTimeout
        module.stop(data)
        module.start(data)
        oldTimeout()
        assertFalse(data.isTimedOut)
        assertTrue(notifications.isEmpty())
        fireTimeout()
        assertTrue(data.isTimedOut)
    }

    @Test
    fun restartSchedulesTimeoutAgain() = runBlocking<Unit> {
        module.start(data)
        module.stop(data)
        module.start(data)
        verify(timer, times(2)).once(SensorService.GPS_READ_TIMEOUT)
        fireTimeout()
        assertTrue(data.isTimedOut)
    }
}
