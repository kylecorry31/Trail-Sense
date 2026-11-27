package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.PressureTide
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
import java.time.Month

internal class WeatherForecasterTest {

    private lateinit var temperatureService: ITemperatureService
    private lateinit var prefs: IWeatherPreferences

    @BeforeEach
    fun setup() = runBlocking {
        temperatureService = mock()
        prefs = mock()
        whenever(prefs.forecastSensitivity).thenReturn(ForecastSensitivity.Medium)
        whenever(prefs.stormSensitivity).thenReturn(ForecastSensitivity.Medium)

        setTemperatures(10f, 20f, 15f)
    }

    @Test
    fun returnsTheMostRecentObservation() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 1013f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertEquals(observations.last(), weather.observation)
    }

    @Test
    fun hasNoObservationWhenThereAreNoReadings() = runBlocking {
        val weather = forecaster().forecast(emptyList(), emptyList(), polarLocation)

        assertNull(weather.observation)
    }

    @Test
    fun returnsTheMostRecentCloudObservationWithinFourHours() = runBlocking {
        val cloud = Reading<CloudGenus?>(CloudGenus.Cumulus, now.minus(Duration.ofHours(3)))

        val weather = forecaster().forecast(emptyList(), listOf(cloud), polarLocation)

        assertEquals(cloud, weather.clouds)
    }

    @Test
    fun ignoresCloudObservationsOlderThanFourHours() = runBlocking {
        val cloud = Reading<CloudGenus?>(CloudGenus.Cumulus, now.minus(Duration.ofHours(5)))

        val weather = forecaster().forecast(emptyList(), listOf(cloud), polarLocation)

        assertNull(weather.clouds)
    }

    @Test
    fun reportsTheThreeHourPressureTendency() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 1012f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertEquals(PressureCharacteristic.Falling, weather.pressureTendency.characteristic)
        // 3 hPa lost over 3 hours
        assertEquals(-3f, weather.pressureTendency.amount, 0.5f)
    }

    @Test
    fun removesTheDailyPressureTideFromTheTendency() = runBlocking {
        val equator = Coordinate(0.0, 0.0)
        val observations = fallingPressure(Duration.ofHours(3), 1013f, 1013f, equator)

        val weather = forecaster().forecast(observations, emptyList(), equator)

        val tideChange = PressureTide.getOffsetHpa(observations.last().time, equator) -
            PressureTide.getOffsetHpa(observations.first().time, equator)
        assertEquals(-tideChange, weather.pressureTendency.amount, 0.01f)
    }

    @Test
    fun ignoresPressureReadingsThatSpanLessThanTenMinutes() = runBlocking {
        // The same drop over 5 minutes is not enough history to be trusted
        val observations = fallingPressure(Duration.ofMinutes(5), 1015f, 1009f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertEquals(PressureCharacteristic.Steady, weather.pressureTendency.characteristic)
        assertEquals(0f, weather.pressureTendency.amount, 0.001f)
    }

    @Test
    fun usesPressureReadingsThatSpanAtLeastTenMinutes() = runBlocking {
        val observations = fallingPressure(Duration.ofMinutes(15), 1015f, 1014.8f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertEquals(PressureCharacteristic.Falling, weather.pressureTendency.characteristic)
    }

    @Test
    fun includesTheTemperaturePredictionFromTheTemperatureService() = runBlocking {
        setTemperatures(low = 4f, high = 24f, current = 18f)

        val weather = forecaster().forecast(emptyList(), emptyList(), polarLocation)

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

        val weather = forecaster().forecast(emptyList(), emptyList(), polarLocation)

        assertTrue(weather.prediction.alerts.contains(WeatherAlert.Cold))
        assertFalse(weather.prediction.alerts.contains(WeatherAlert.Hot))
    }

    @Test
    fun alertsWhenTheDayWillBeHot() = runBlocking {
        setTemperatures(low = 25f, high = 35f, current = 30f)

        val weather = forecaster().forecast(emptyList(), emptyList(), polarLocation)

        assertTrue(weather.prediction.alerts.contains(WeatherAlert.Hot))
        assertFalse(weather.prediction.alerts.contains(WeatherAlert.Cold))
    }

    @Test
    fun doesNotAlertForComfortableTemperatures() = runBlocking {
        setTemperatures(low = 10f, high = 20f, current = 15f)

        val weather = forecaster().forecast(emptyList(), emptyList(), polarLocation)

        assertEquals(emptyList<WeatherAlert>(), weather.prediction.alerts)
    }

    @Test
    fun alertsWhenAStormIsPredicted() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 985f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertTrue(
            weather.hourly.contains(WeatherCondition.Storm),
            "Expected a storm, got ${weather.hourly}"
        )
        assertTrue(weather.prediction.alerts.contains(WeatherAlert.Storm))
    }

    @Test
    fun aStormArrivesAtAKnownTime() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 985f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertNotNull(weather.prediction.hourlyArrival)
    }

    @Test
    fun doesNotLookUpTheTemperatureRangeWhenNoPrecipitationIsPredicted() = runBlocking {
        forecaster().forecast(emptyList(), emptyList(), polarLocation)

        verify(temperatureService, never()).getTemperatureRange(any(), any())
    }

    @Test
    fun looksUpTheTemperatureRangeWhenPrecipitationIsPredicted() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(3), 1015f, 985f)

        forecaster().forecast(observations, emptyList(), polarLocation)

        verify(temperatureService).getTemperatureRange(any(), any())
    }

    @Test
    fun steadyHighPressureIsClear() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(12), 1025f, 1025f)

        val weather = forecaster().forecast(observations, emptyList(), polarLocation)

        assertEquals(listOf(WeatherCondition.Clear), weather.hourly)
    }

    @Test
    fun theForecastPredictsPrecipitationFromAFastFallingLow() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(12), 1010f, 995f, temperateLocation)

        val weather = forecaster().forecast(observations, emptyList(), temperateLocation)

        assertTrue(WeatherCondition.Precipitation in weather.hourly)
        assertTrue(WeatherCondition.Rain in weather.hourly)
    }

    @Test
    fun theForecastDoesNotPredictPrecipitationFromSteadyHighPressure() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(24), 1030f, 1030f, temperateLocation)

        val weather = forecaster().forecast(observations, emptyList(), temperateLocation)

        assertFalse(WeatherCondition.Precipitation in weather.hourly)
    }

    @Test
    fun aMoreSensitiveForecastPredictsPrecipitationFromSmallerFalls() = runBlocking {
        val falls = ForecastSensitivity.entries.map { sensitivity ->
            whenever(prefs.forecastSensitivity).thenReturn(sensitivity)
            countFallsPredicting(WeatherCondition.Precipitation)
        }

        assertTrue(falls[0] < falls[1] && falls[1] < falls[2], falls.toString())
    }

    @Test
    fun aMoreSensitiveForecastPredictsStormsFromSmallerFalls() = runBlocking {
        val falls = ForecastSensitivity.entries.map { sensitivity ->
            whenever(prefs.stormSensitivity).thenReturn(sensitivity)
            countFallsPredicting(WeatherCondition.Storm)
        }

        assertTrue(falls[0] < falls[1] && falls[1] < falls[2], falls.toString())
    }

    @Test
    fun theForecastGivesTheProbabilityOfItsOwnConditionsAndThePrecipitationType() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(12), 1010f, 995f, temperateLocation)

        val weather = forecaster().forecast(observations, emptyList(), temperateLocation)

        val probabilities = weather.probabilities
        assertTrue(WeatherCondition.Precipitation in probabilities)
        assertTrue(probabilities.values.all { it in 0f..1f })
        assertEquals(probabilities[WeatherCondition.Precipitation], probabilities[WeatherCondition.Rain])
    }

    @Test
    fun theForecastHasNoSkyProbabilityWhenACloudSetsTheSky() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(24), 1030f, 1030f, temperateLocation)
        val cloud = Reading<CloudGenus?>(CloudGenus.Stratus, now.minus(Duration.ofMinutes(30)))

        val weather = forecaster().forecast(observations, listOf(cloud), temperateLocation)

        assertFalse(WeatherCondition.Overcast in weather.probabilities)
        assertFalse(WeatherCondition.Clear in weather.probabilities)
    }

    @Test
    fun theForecastUsesALoggedCloudForTheSky() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(24), 1030f, 1030f, temperateLocation)
        val cloud = Reading<CloudGenus?>(CloudGenus.Stratus, now.minus(Duration.ofMinutes(30)))

        val weather = forecaster().forecast(observations, listOf(cloud), temperateLocation)

        assertTrue(WeatherCondition.Overcast in weather.hourly)
        assertFalse(WeatherCondition.Clear in weather.hourly)
    }

    @Test
    fun predictsSnowWhenItWillBeBelowFreezing() = runBlocking {
        setTemperatures(low = -10f, high = -2f, current = -5f)
        val observations = fallingPressure(Duration.ofHours(12), 1010f, 995f, temperateLocation)

        val weather = forecaster().forecast(observations, emptyList(), temperateLocation)

        assertTrue(WeatherCondition.Snow in weather.hourly)
        assertFalse(WeatherCondition.Rain in weather.hourly)
    }

    @Test
    fun withoutAClimateOnlyLoggedCloudsGiveConditions() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(12), 1010f, 995f, temperateLocation)
        val cloud = Reading<CloudGenus?>(CloudGenus.Stratus, now.minus(Duration.ofMinutes(30)))
        val forecaster = WeatherForecaster(temperatureService, prefs)

        val withoutClouds = forecaster.forecast(observations, emptyList(), temperateLocation)
        val withClouds = forecaster.forecast(observations, listOf(cloud), temperateLocation)

        assertEquals(emptyList<WeatherCondition>(), withoutClouds.hourly)
        assertTrue(WeatherCondition.Overcast in withClouds.hourly)
        assertEquals(emptyMap<WeatherCondition, Float>(), withClouds.probabilities)
    }

    @Test
    fun theForecastNeedsTwoHoursOfHistory() = runBlocking {
        val observations = fallingPressure(Duration.ofHours(1), 1010f, 990f, temperateLocation)

        val weather = forecaster().forecast(observations, emptyList(), temperateLocation)

        assertEquals(emptyList<WeatherCondition>(), weather.hourly)
    }

    private val CurrentWeather.hourly: List<WeatherCondition>
        get() = prediction.hourly.map { it.condition }

    private val CurrentWeather.probabilities: Map<WeatherCondition, Float>
        get() = prediction.hourly.mapNotNull { (condition, probability) -> probability?.let { condition to it } }.toMap()

    private fun forecaster(): WeatherForecaster {
        return WeatherForecaster(temperatureService, prefs, climate = climate(80f))
    }

    private suspend fun countFallsPredicting(condition: WeatherCondition): Int {
        val forecaster = forecaster()
        return (0..60).count { step ->
            val observations = fallingPressure(Duration.ofHours(12), 1015f, 1015f - step / 2f, temperateLocation)
            condition in forecaster.forecast(observations, emptyList(), temperateLocation).hourly
        }
    }

    private fun climate(monthlyPrecipitationMm: Float): LocalClimate {
        return LocalClimate(
            Month.entries.associateWith { Distance.from(monthlyPrecipitationMm, DistanceUnits.Millimeters) },
            Range(Temperature.celsius(10f), Temperature.celsius(20f))
        )
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
        to: Float,
        location: Coordinate = polarLocation
    ): List<WeatherObservation> {
        val steps = 10
        return (0..steps).map {
            val percent = it / steps.toFloat()
            WeatherObservation(
                it.toLong(),
                now.minus(span).plus(Duration.ofMillis((span.toMillis() * percent).toLong())),
                Pressure.hpa(from + (to - from) * percent),
                Temperature.celsius(15f),
                50f,
                location
            )
        }
    }

    companion object {
        private val polarLocation = Coordinate(90.0, 0.0)
        private val temperateLocation = Coordinate(42.0, -71.0)
        private val now: Instant get() = Instant.now()
    }
}
