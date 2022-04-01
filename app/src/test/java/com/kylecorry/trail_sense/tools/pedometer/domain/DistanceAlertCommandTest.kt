package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.IAlerter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IStepCounter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class DistanceAlertCommandTest {

    private lateinit var command: DistanceAlertCommand
    private lateinit var prefs: IPedometerPreferences
    private lateinit var counter: IStepCounter
    private lateinit var calculator: IPaceCalculator
    private lateinit var alerter: IAlerter

    @BeforeEach
    fun setup(){
        prefs = mock()
        counter = mock()
        calculator = mock()
        alerter = mock()

        whenever(counter.steps).thenReturn(100)
        command = DistanceAlertCommand(prefs, counter, calculator, alerter)
    }

    @Test
    fun doesNotAlertWhenNoDistanceSet() {
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
    fun doesNotAlertWhenDistanceNotReached() {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(Distance.meters(100f))
        whenever(calculator.distance(100)).thenReturn(Distance.meters(99f))

        // Act
        command.execute()

        // Assert
        verify(alerter, never()).alert()
        verify(prefs, never()).alertDistance = null
    }

    @Test
    fun alertsWhenDistanceReached() {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(Distance.meters(100f))
        whenever(calculator.distance(100)).thenReturn(Distance.meters(100f))

        // Act
        command.execute()

        // Assert
        verify(alerter).alert()
        verify(prefs).alertDistance = null
    }

    @Test
    fun alertsWhenDistanceExceeded() {
        // Arrange
        whenever(prefs.alertDistance).thenReturn(Distance.meters(100f))
        whenever(calculator.distance(100)).thenReturn(Distance.meters(101f))

        // Act
        command.execute()

        // Assert
        verify(alerter).alert()
        verify(prefs).alertDistance = null
    }
}