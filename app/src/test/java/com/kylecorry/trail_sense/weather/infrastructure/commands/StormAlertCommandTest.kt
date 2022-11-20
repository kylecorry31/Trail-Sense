package com.kylecorry.trail_sense.weather.infrastructure.commands

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.HourlyArrivalTime
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPrediction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

internal class StormAlertCommandTest {
    private lateinit var command: StormAlertCommand
    private lateinit var flag: Flag
    private lateinit var prefs: IWeatherPreferences
    private lateinit var alerter: IDismissibleAlerter

    @BeforeEach
    fun setup() {
        flag = mock()
        prefs = mock()
        alerter = mock()

        whenever(prefs.sendStormAlerts).thenReturn(true)
        whenever(prefs.shouldMonitorWeather).thenReturn(true)
        whenever(flag.get()).thenReturn(false)

        command = StormAlertCommand(flag, prefs, alerter)
    }

    @Test
    fun doesNotAlertIfNoStorm() {
        command.execute(weather(WeatherCondition.Precipitation))
        verify(flag, times(1)).set(false)
        verify(alerter, never()).alert()
        verify(alerter, times(1)).dismiss()
    }

    @Test
    fun doesNotAlertIfAlreadySent() {
        whenever(flag.get()).thenReturn(true)
        command.execute(weather(WeatherCondition.Storm))
        verify(flag, never()).set(any())
        verify(alerter, never()).alert()
        verify(alerter, never()).dismiss()
    }

    @Test
    fun doesNotAlertIfAlertsOff() {
        whenever(prefs.sendStormAlerts).thenReturn(false)
        command.execute(weather(WeatherCondition.Storm))
        verify(flag, never()).set(any())
        verify(alerter, never()).alert()
        verify(alerter, never()).dismiss()
    }

    @Test
    fun doesNotAlertIfMonitorOff() {
        whenever(prefs.shouldMonitorWeather).thenReturn(false)
        command.execute(weather(WeatherCondition.Storm))
        verify(flag, never()).set(any())
        verify(alerter, never()).alert()
        verify(alerter, never()).dismiss()
    }

    @Test
    fun alertsIfStorm() {
        command.execute(weather(WeatherCondition.Storm))
        verify(flag, times(1)).set(true)
        verify(alerter, times(1)).alert()
        verify(alerter, never()).dismiss()
    }

    private fun weather(vararg conditions: WeatherCondition): CurrentWeather {
        return CurrentWeather(
            WeatherPrediction(conditions.toList(), emptyList(), null, HourlyArrivalTime.Now, null),
            PressureTendency(PressureCharacteristic.Steady, 0f),
            null,
            null
        )
    }
}