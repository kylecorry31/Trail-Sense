package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.WeatherAlert
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant

internal class WeatherForecasterTest {

    private lateinit var temperatureService: ITemperatureService
    private lateinit var prefs: IWeatherPreferences

    @BeforeEach
    fun setup() = runBlocking {
        temperatureService = mock()
        prefs = mock()

        whenever(prefs.stormAlertThreshold).thenReturn(-6f)
        whenever(prefs.hourlyForecastChangeThreshold).thenReturn(1.5f)
        whenever(prefs.forecastSource).thenReturn(ForecastSource.Sol)

        setTemperatures(10f, 20f, 15f)
    }

    @Test
    fun returnsTheMostRecentObservation() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 1013f)

        val weather = forecaster().forecast(observations, emptyList(), location)

        assertEquals(observations.last(), weather.observation)
    }

    @Test
    fun hasNoObservationWhenThereAreNoReadings() = runBlocking {
        val weather = forecaster().forecast(emptyList(), emptyList(), location)

        assertNull(weather.observation)
    }

    @Test
    fun returnsTheMostRecentCloudObservationWithinFourHours() = runBlocking {
        val cloud = Reading<CloudGenus?>(CloudGenus.Cumulus, now.minus(Duration.ofHours(3)))

        val weather = forecaster().forecast(emptyList(), listOf(cloud), location)

        assertEquals(cloud, weather.clouds)
    }

    @Test
    fun ignoresCloudObservationsOlderThanFourHours() = runBlocking {
        val cloud = Reading<CloudGenus?>(CloudGenus.Cumulus, now.minus(Duration.ofHours(5)))

        val weather = forecaster().forecast(emptyList(), listOf(cloud), location)

        assertNull(weather.clouds)
    }

    @Test
    fun reportsTheThreeHourPressureTendency() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 1012f)

        val weather = forecaster().forecast(observations, emptyList(), location)

        assertEquals(PressureCharacteristic.Falling, weather.pressureTendency.characteristic)
        // 3 hPa lost over 3 hours
        assertEquals(-3f, weather.pressureTendency.amount, 0.5f)
    }

    @Test
    fun ignoresPressureReadingsThatSpanLessThanTenMinutes() = runBlocking {
        // The same drop over 5 minutes is not enough history to be trusted
        val observations = fallingPressure(Duration.ofMinutes(5), 1015f, 1009f)

        val weather = forecaster().forecast(observations, emptyList(), location)

        assertEquals(PressureCharacteristic.Steady, weather.pressureTendency.characteristic)
        assertEquals(0f, weather.pressureTendency.amount, 0.001f)
    }

    @Test
    fun usesPressureReadingsThatSpanAtLeastTenMinutes() = runBlocking {
        val observations = fallingPressure(Duration.ofMinutes(15), 1015f, 1014.8f)

        val weather = forecaster().forecast(observations, emptyList(), location)

        assertEquals(PressureCharacteristic.Falling, weather.pressureTendency.characteristic)
    }

    @Test
    fun includesTheTemperaturePredictionFromTheTemperatureService() = runBlocking {
        setTemperatures(low = 4f, high = 24f, current = 18f)

        val weather = forecaster().forecast(emptyList(), emptyList(), location)

        val temperature = weather.prediction.temperature
        assertNotNull(temperature)
        assertEquals(4f, temperature!!.low.celsius().value, 0.001f)
        assertEquals(24f, temperature.high.celsius().value, 0.001f)
        assertEquals(18f, temperature.current.celsius().value, 0.001f)
        assertEquals(14f, temperature.average.celsius().value, 0.001f)
    }

    @Test
    fun alertsWhenTheDayWillBeCold() = runBlocking {
        setTemperatures(low = 0f, high = 10f, current = 5f)

        val weather = forecaster().forecast(emptyList(), emptyList(), location)

        assertTrue(weather.prediction.alerts.contains(WeatherAlert.Cold))
        assertFalse(weather.prediction.alerts.contains(WeatherAlert.Hot))
    }

    @Test
    fun alertsWhenTheDayWillBeHot() = runBlocking {
        setTemperatures(low = 25f, high = 35f, current = 30f)

        val weather = forecaster().forecast(emptyList(), emptyList(), location)

        assertTrue(weather.prediction.alerts.contains(WeatherAlert.Hot))
        assertFalse(weather.prediction.alerts.contains(WeatherAlert.Cold))
    }

    @Test
    fun doesNotAlertForComfortableTemperatures() = runBlocking {
        setTemperatures(low = 10f, high = 20f, current = 15f)

        val weather = forecaster().forecast(emptyList(), emptyList(), location)

        assertEquals(emptyList<WeatherAlert>(), weather.prediction.alerts)
    }

    @Test
    fun alertsWhenAStormIsPredicted() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 985f)

        val weather = forecaster().forecast(observations, emptyList(), location)

        assertTrue(
            weather.prediction.hourly.contains(WeatherCondition.Storm),
            "Expected a storm, got ${weather.prediction.hourly}"
        )
        assertTrue(weather.prediction.alerts.contains(WeatherAlert.Storm))
    }

    @Test
    fun aStormArrivesAtAKnownTime() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 985f)

        val weather = forecaster().forecast(observations, emptyList(), location)

        assertNotNull(weather.prediction.hourlyArrival)
    }

    @Test
    fun doesNotLookUpTheTemperatureRangeWhenNoPrecipitationIsPredicted() = runBlocking {
        forecaster().forecast(emptyList(), emptyList(), location)

        verify(temperatureService, never()).getTemperatureRange(any(), any())
    }

    @Test
    fun looksUpTheTemperatureRangeWhenPrecipitationIsPredicted() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 985f)

        forecaster().forecast(observations, emptyList(), location)

        verify(temperatureService).getTemperatureRange(any(), any())
    }

    private fun forecaster(): WeatherForecaster {
        return WeatherForecaster(temperatureService, prefs)
    }

    private suspend fun setTemperatures(low: Float, high: Float, current: Float) {
        val range = Range(Temperature.celsius(low), Temperature.celsius(high))
        whenever(temperatureService.getTemperatureRange(any())).thenReturn(range)
        whenever(temperatureService.getTemperatureRange(any(), any())).thenReturn(range)
        whenever(temperatureService.getTemperature(any())).thenReturn(Temperature.celsius(current))
    }

    private fun fallingPressure(
        span: Duration,
        from: Float,
        to: Float
    ): List<WeatherObservation> {
        val steps = 10
        return (0..steps).map {
            val percent = it / steps.toFloat()
            WeatherObservation(
                it.toLong(),
                now.minus(span).plus(Duration.ofMillis((span.toMillis() * percent).toLong())),
                Pressure.hpa(from + (to - from) * percent),
                Temperature.celsius(15f),
                50f
            )
        }
    }

    companion object {
        private val location = Coordinate(40.0, -80.0)
        private val now: Instant get() = Instant.now()
    }
}
