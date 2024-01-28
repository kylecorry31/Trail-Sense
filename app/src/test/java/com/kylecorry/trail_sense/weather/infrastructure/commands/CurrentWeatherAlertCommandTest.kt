package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

internal class CurrentWeatherAlertCommandTest {

    private lateinit var command: CurrentWeatherAlertCommand
    private lateinit var prefs: IWeatherPreferences
    private lateinit var alerter: IValueAlerter<CurrentWeather>
    private val weather = CurrentWeather(
        WeatherPrediction(
            emptyList(),
            emptyList(),
            null,
            WeatherArrivalTime(Instant.now(), false),
            null,
            emptyList()
        ),
        PressureTendency(PressureCharacteristic.Steady, 0f),
        null,
        null
    )

    @BeforeEach
    fun setup(){
        prefs = mock()
        alerter = mock()

        whenever(prefs.shouldMonitorWeather).thenReturn(true)

        command = CurrentWeatherAlertCommand(prefs, alerter)
    }

    @Test
    fun shouldNotAlertIfWeatherMonitorOff(){
        whenever(prefs.shouldMonitorWeather).thenReturn(false)

        command.execute(weather)

        verify(alerter, never()).alert(any())
    }
    @Test
    fun shouldAlert(){
        command.execute(weather)

        verify(alerter, times(1)).alert(any())
    }

}