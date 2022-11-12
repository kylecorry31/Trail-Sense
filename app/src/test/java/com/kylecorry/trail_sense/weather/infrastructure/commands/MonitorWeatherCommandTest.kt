package com.kylecorry.trail_sense.weather.infrastructure.commands

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherFront
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.database.IReadingRepo
import com.kylecorry.trail_sense.weather.domain.RawWeatherObservation
import com.kylecorry.trail_sense.weather.infrastructure.CurrentWeather
import com.kylecorry.trail_sense.weather.infrastructure.HourlyArrivalTime
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPrediction
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.IWeatherSubsystem
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

internal class MonitorWeatherCommandTest {

    private lateinit var monitor: MonitorWeatherCommand
    private lateinit var repo: IReadingRepo<RawWeatherObservation>
    private lateinit var observer: IWeatherObserver
    private lateinit var subsystem: IWeatherSubsystem
    private lateinit var alerter: IValueAlerter<CurrentWeather>

    @BeforeEach
    fun setup() {
        repo = mock()
        observer = mock()
        subsystem = mock()
        alerter = mock()
        monitor = MonitorWeatherCommand(repo, observer, subsystem, alerter)
    }

    @Test
    fun canRecordWeather() = runBlocking {
        val weather = CurrentWeather(
            WeatherPrediction(emptyList(), emptyList(), WeatherFront.Warm, HourlyArrivalTime.Now),
            PressureTendency(PressureCharacteristic.Falling, -1f),
            null,
            null
        )
        val observation = Reading(RawWeatherObservation(0, 1000f, 0f, 0f), Instant.now())

        whenever(observer.getWeatherObservation()).thenReturn(observation)
        whenever(subsystem.getWeather()).thenReturn(weather)
        whenever(repo.add(observation)).thenReturn(1L)

        monitor.execute()

        verify(repo, times(1)).add(observation)
        verify(alerter, times(1)).alert(weather)
    }

}