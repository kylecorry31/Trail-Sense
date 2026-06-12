package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

internal class DistanceAlertCommandTest {

    private lateinit var command: DistanceAlertCommand
    private lateinit var prefs: IPedometerPreferences
    private lateinit var stepTrackerService: IStepTrackerService
    private lateinit var calculator: IPaceCalculator
    private lateinit var alerter: IAlerter

    @BeforeEach
    fun setup() {
        prefs = mock()
        stepTrackerService = mock()
        calculator = mock()
        alerter = mock()

        command = DistanceAlertCommand(prefs, stepTrackerService, calculator, alerter)
    }

    @Test
    fun doesNotAlertWhenNoDistanceSet() = runBlocking {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(null)
        whenever(calculator.distance(100)).thenReturn(Distance.meters(100f))

        // Act
        command.execute()

        // Assert
        verify(alerter, never()).alert()
        verify(prefs, never()).alertDistance = null
    }

    @Test
    fun doesNotAlertWhenDistanceNotReached() = runBlocking {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(Distance.meters(100f))
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(getOpenPeriod(100))
        whenever(calculator.distance(100)).thenReturn(Distance.meters(99f))

        // Act
        command.execute()

        // Assert
        verify(alerter, never()).alert()
        verify(prefs, never()).alertDistance = null
    }

    @Test
    fun alertsWhenDistanceReached() = runBlocking {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(Distance.meters(100f))
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(getOpenPeriod(100))
        whenever(calculator.distance(100)).thenReturn(Distance.meters(100f))

        // Act
        command.execute()

        // Assert
        verify(alerter).alert()
        verify(prefs).alertDistance = null
    }

    @Test
    fun alertsWhenDistanceExceeded() = runBlocking {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(Distance.meters(100f))
        whenever(stepTrackerService.getOpenStepTrackingPeriod()).thenReturn(getOpenPeriod(100))
        whenever(calculator.distance(100)).thenReturn(Distance.meters(101f))

        // Act
        command.execute()

        // Assert
        verify(alerter).alert()
        verify(prefs).alertDistance = null
    }

    private fun getOpenPeriod(steps: Long): StepTrackingPeriod {
        val time = Instant.ofEpochMilli(100)
        return StepTrackingPeriod(
            0,
            time,
            null,
            listOf(
                StepCountBucket(
                    0,
                    0,
                    time,
                    time,
                    steps
                )
            )
        )
    }
}
