package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.alerts.WeatherAlertGenerator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.model.WeatherModel
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.model.LoggedSky
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.model.WeatherModelPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.getLastCloud
import com.kylecorry.trail_sense.tools.weather.domain.getTemperaturePrediction
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

internal class WeatherForecaster(
    private val temperatureService: ITemperatureService,
    prefs: IWeatherPreferences,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val climate: LocalClimate? = null
) : IWeatherForecaster {

    private val forecastSensitivity = prefs.forecastSensitivity
    private val stormSensitivity = prefs.stormSensitivity
    private val updateFrequency = prefs.weatherUpdateFrequency
    private val alertGenerator = WeatherAlertGenerator()
    private val primarySelector = PrimaryWeatherSelector()

    override suspend fun forecast(
        observations: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): CurrentWeather {
        val forecast = getForecast(observations, clouds, location)
        val last = observations.lastOrNull()
        val lastCloud = clouds.getLastCloud(Duration.ofHours(4))

        val weather = CurrentWeather(
            WeatherPrediction(
                forecast.conditions,
                forecast.laterConditions,
                forecast.arrival,
                temperatureService.getTemperaturePrediction(ZonedDateTime.now(clock)),
                emptyList()
            ),
            forecast.pressureTendency,
            last,
            lastCloud
        )

        val alerts = alertGenerator.getAlerts(weather, forecast.isStormAlert)

        return weather.copy(prediction = weather.prediction.copy(alerts = alerts))
    }

    private fun getForecast(
        readings: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): Forecast {
        val pressures = if (hasEnoughReadings(readings)) {
            readings.map { removeTide(it.pressureReading(), it.location) }
        } else {
            emptyList()
        }

        val prediction = getPrediction(pressures, clouds, location)
        val conditions = withPrecipitationOvercast(prediction?.conditions.orEmpty())
        val laterConditions = withPrecipitationOvercast(prediction?.laterConditions.orEmpty())
        val primary = primarySelector.getWeather(conditions)
        val arrival = conditions.firstOrNull { it.condition == primary }?.arrival
            ?: conditions.filter { it.condition in WEATHER_CONDITIONS }.mapNotNull { it.arrival }.minOrNull()

        val precipitationType = prediction?.let {
            getPrecipitationType(conditions, it.isPrecipitationSnow, it.snowChance)
        }
        val laterPrecipitationType = prediction?.let {
            getPrecipitationType(laterConditions, it.isLaterPrecipitationSnow, it.laterSnowChance)
        }

        return Forecast(
            WeatherModel.getTendency(pressures),
            conditions + listOfNotNull(precipitationType),
            laterConditions + listOfNotNull(laterPrecipitationType),
            arrival,
            prediction?.isStormAlert == true
        )
    }

    /**
     * @return null without a climate, or without a recent enough pressure history or logged sky
     */
    private fun getPrediction(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): WeatherModelPrediction? {
        climate ?: return null
        val now = Instant.now(clock)
        val recentPressures = if (isRecent(pressures.lastOrNull())) pressures else emptyList()
        return WeatherModel(climate, location, forecastSensitivity, stormSensitivity)
            .predict(recentPressures, LoggedSky.fromLoggedClouds(clouds, now), now)
    }

    private fun isRecent(reading: Reading<Pressure>?): Boolean {
        reading ?: return false
        val maxAge = maxOf(MIN_MAX_READING_AGE, updateFrequency.multipliedBy(2))
        return Duration.between(reading.time, Instant.now(clock)) <= maxAge
    }

    private fun withPrecipitationOvercast(conditions: List<ForecastCondition>): List<ForecastCondition> {
        val precipitation = conditions.firstOrNull { it.condition == WeatherCondition.Precipitation }
            ?: return conditions
        val overcast = conditions.firstOrNull { it.condition == WeatherCondition.Overcast } ?: precipitation
        return conditions.filter { it.condition !in SKY_CONDITIONS } + ForecastCondition(
            WeatherCondition.Overcast,
            overcast.probability,
            precipitation.arrival,
            overcast.relativeProbability
        )
    }

    /**
     * @param snowChance the chance from 0 to 1 that the precipitation is mostly snow or ice
     */
    private fun getPrecipitationType(
        conditions: List<ForecastCondition>,
        isSnow: Boolean,
        snowChance: Float
    ): ForecastCondition? {
        val precipitation = conditions.firstOrNull { it.condition == WeatherCondition.Precipitation } ?: return null
        val typeChance = if (isSnow) snowChance else 1 - snowChance
        return precipitation.copy(
            condition = if (isSnow) WeatherCondition.Snow else WeatherCondition.Rain,
            probability = precipitation.probability?.times(typeChance)
        )
    }

    private fun removeTide(reading: Reading<Pressure>, location: Coordinate): Reading<Pressure> {
        val tide = Meteorology.getPressureTide(reading.time, location).hpa().value
        val pressure = Pressure.hpa(reading.value.hpa().value - tide).convertTo(reading.value.units)
        return reading.copy(value = pressure)
    }

    private fun hasEnoughReadings(readings: List<WeatherObservation>): Boolean {
        val start = readings.minOfOrNull { it.time } ?: return false
        val end = readings.maxOf { it.time }
        return Duration.between(start, end) >= MIN_DURATION
    }

    private data class Forecast(
        val pressureTendency: PressureTendency,
        val conditions: List<ForecastCondition>,
        val laterConditions: List<ForecastCondition>,
        val arrival: RelativeArrivalTime?,
        val isStormAlert: Boolean
    )

    companion object {
        private val MIN_DURATION = Duration.ofMinutes(10)
        private val MIN_MAX_READING_AGE = Duration.ofHours(3)
        private val SKY_CONDITIONS = setOf(WeatherCondition.Clear, WeatherCondition.Overcast)
        private val WEATHER_CONDITIONS = setOf(
            WeatherCondition.Precipitation,
            WeatherCondition.Storm,
            WeatherCondition.Wind,
            WeatherCondition.Thunderstorm
        )
    }
}
