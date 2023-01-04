package com.kylecorry.trail_sense.weather.infrastructure.commands

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.domain.WeatherAlert
import com.kylecorry.trail_sense.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

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
        command.execute(weather(false))
        verify(flag, times(1)).set(false)
        verify(alerter, never()).alert()
        verify(alerter, times(1)).dismiss()
    }

    @Test
    fun doesNotAlertIfAlreadySent() {
        whenever(flag.get()).thenReturn(true)
        command.execute(weather(true))
        verify(flag, never()).set(any())
        verify(alerter, never()).alert()
        verify(alerter, never()).dismiss()
    }

    @Test
    fun doesNotAlertIfAlertsOff() {
        whenever(prefs.sendStormAlerts).thenReturn(false)
        command.execute(weather(true))
        verify(flag, never()).set(any())
        verify(alerter, never()).alert()
        verify(alerter, never()).dismiss()
    }

    @Test
    fun doesNotAlertIfMonitorOff() {
        whenever(prefs.shouldMonitorWeather).thenReturn(false)
        command.execute(weather(true))
        verify(flag, never()).set(any())
        verify(alerter, never()).alert()
        verify(alerter, never()).dismiss()
    }

    @Test
    fun alertsIfStorm() {
        command.execute(weather(true))
        verify(flag, times(1)).set(true)
        verify(alerter, times(1)).alert()
        verify(alerter, never()).dismiss()
    }

    private fun weather(hasStorm: Boolean): CurrentWeather {
        return CurrentWeather(
            WeatherPrediction(
                emptyList(),
                emptyList(),
                null,
                WeatherArrivalTime(Instant.now(), false),
                null,
                if (hasStorm) listOf(WeatherAlert.Storm) else emptyList()
            ),
            PressureTendency(PressureCharacteristic.Steady, 0f),
            null,
            null
        )
    }
}