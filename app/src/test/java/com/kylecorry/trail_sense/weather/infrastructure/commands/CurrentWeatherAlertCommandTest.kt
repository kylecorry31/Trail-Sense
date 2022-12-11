package com.kylecorry.trail_sense.weather.infrastructure.commands

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.weather.domain.HourlyArrivalTime
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.domain.WeatherPrediction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

internal class CurrentWeatherAlertCommandTest {

    private lateinit var command: CurrentWeatherAlertCommand
    private lateinit var prefs: IWeatherPreferences
    private lateinit var alerter: IValueAlerter<CurrentWeather>
    private val weather = CurrentWeather(
        WeatherPrediction(emptyList(), emptyList(), null, HourlyArrivalTime.Now, null),
        PressureTendency(PressureCharacteristic.Steady, 0f),
        null,
        null
    )

    @BeforeEach
    fun setup(){
        prefs = mock()
        alerter = mock()

        whenever(prefs.shouldMonitorWeather).thenReturn(true)
        whenever(prefs.shouldShowWeatherNotification).thenReturn(true)

        command = CurrentWeatherAlertCommand(prefs, alerter)
    }

    @Test
    fun shouldNotAlertIfWeatherMonitorOff(){
        whenever(prefs.shouldMonitorWeather).thenReturn(false)

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldNotAlertIfNotificationDisabled(){
        whenever(prefs.shouldShowWeatherNotification).thenReturn(false)

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }

    @Test
    fun shouldAlert(){
        command.execute(weather)

        verify(alerter, times(1)).alert(any())
    }

}