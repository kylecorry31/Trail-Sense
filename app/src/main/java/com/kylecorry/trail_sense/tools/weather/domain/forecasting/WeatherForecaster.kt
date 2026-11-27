package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.ForecastClimate
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.PressureTide
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.alerts.WeatherAlertGenerator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTimeCalculator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned.LearnedWeatherModel
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.get3hTendency
import com.kylecorry.trail_sense.tools.weather.domain.getLastCloud
import com.kylecorry.trail_sense.tools.weather.domain.getTemperaturePrediction
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

internal class WeatherForecaster(
    private val temperatureService: ITemperatureService,
    prefs: IWeatherPreferences,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val climate: LocalClimate? = null,
    private val useLearnedModel: Boolean = false
) : IWeatherForecaster {

    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold
    private val arrivalCalculator = WeatherArrivalTimeCalculator(clock)
    private val alertGenerator = WeatherAlertGenerator()

    override suspend fun forecast(
        observations: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): CurrentWeather {
        val (arrival, forecast) = getForecast(observations, clouds, location)
        val last = observations.lastOrNull()
        val lastCloud = clouds.getLastCloud(Duration.ofHours(4))

        val tendency = getTendency(forecast)

        val weather = CurrentWeather(
            WeatherPrediction(
                forecast.first().conditions,
                forecast.last().conditions,
                forecast.first().front,
                arrival,
                temperatureService.getTemperaturePrediction(ZonedDateTime.now(clock)),
                emptyList()
            ),
            tendency,
            last,
            lastCloud
        )

        val alerts = alertGenerator.getAlerts(weather)

        return weather.copy(prediction = weather.prediction.copy(alerts = alerts))
    }

    private suspend fun getForecast(
        readings: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): Pair<WeatherArrivalTime?, List<WeatherForecast>> {
        val pressures = if (!hasEnoughReadings(readings)) {
            emptyList()
        } else {
            readings.map { PressureTide.remove(it.pressureReading(), it.location) }
        }

        val model = climate?.takeIf { useLearnedModel }?.let { LearnedWeatherModel(it, location) }
        val forecast = if (model == null) {
            getSolForecast(pressures, clouds, location, climate)
        } else {
            getLearnedForecast(model, pressures, clouds, location)
        }
        val arrival = arrivalCalculator.getArrivalTime(forecast, clouds)

        // WHY: Temperatures only matter for precipitation, so skip fetching them otherwise
        if (forecast.none { it.conditions.contains(WeatherCondition.Precipitation) }) {
            return arrival to forecast
        }

        val arrivesIn = arrival?.let { Duration.between(Instant.now(clock), it.time) } ?: Duration.ZERO
        val temperatures = getHighLowTemperature(ZonedDateTime.now(clock), arrivesIn, Duration.ofHours(6))
        if (model == null) {
            // WHY: Sol needs the temperatures during the precipitation, which depend on the arrival time it predicts
            return arrival to getSolForecast(pressures, clouds, location, climate, temperatures)
        }
        return arrival to forecast.map { it.copy(conditions = model.addTemperatureConditions(it.conditions, temperatures)) }
    }

    private fun getLearnedForecast(
        model: LearnedWeatherModel,
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): List<WeatherForecast> {
        val solForecast = getSolForecast(pressures, clouds, location, null)
        val cloudForecast = if (clouds.isEmpty()) null else getSolForecast(emptyList(), clouds, location, null)
        val conditions = combine(model.getConditions(pressures), cloudForecast?.first()?.conditions.orEmpty())
        val laterConditions = combine(model.getLaterConditions(pressures), cloudForecast?.last()?.conditions.orEmpty())
        val current = solForecast.first().copy(
            conditions = conditions,
            time = getConditionTime(conditions, cloudForecast?.first())
        )
        return listOf(current, solForecast.last().copy(conditions = laterConditions))
    }

    private fun combine(
        pressureConditions: List<WeatherCondition>,
        cloudConditions: List<WeatherCondition>
    ): List<WeatherCondition> {
        val cloudSky = cloudConditions.filter { it in SKY_CONDITIONS }
        val sky = cloudSky.ifEmpty { pressureConditions.filter { it in SKY_CONDITIONS } }
        val weather = (pressureConditions + cloudConditions).filter { it in WEATHER_CONDITIONS }.distinct()
        return if (WeatherCondition.Precipitation in weather) {
            weather + WeatherCondition.Overcast
        } else {
            sky + weather
        }
    }

    private fun getConditionTime(
        conditions: List<WeatherCondition>,
        cloudForecast: WeatherForecast?
    ): Instant? {
        if (WeatherCondition.Precipitation in conditions) {
            return cloudForecast?.time?.takeIf { WeatherCondition.Precipitation in cloudForecast.conditions }
        }
        return Instant.now(clock).takeIf { conditions.isNotEmpty() && conditions.none { it == WeatherCondition.Storm } }
    }

    private fun hasEnoughReadings(readings: List<WeatherObservation>): Boolean {
        val times = readings.map { it.time }
        val start = times.minOrNull() ?: return false
        val end = times.maxOrNull() ?: return false
        return Duration.between(start, end) >= minDuration
    }

    private fun getSolForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate,
        climate: LocalClimate?,
        temperatures: Range<Temperature>? = null
    ): List<WeatherForecast> {
        val now = Instant.now(clock)
        val forecastClimate = climate?.getForecastClimate(now, temperatures)
            ?: temperatures?.let { ForecastClimate(it) }
        return Meteorology.forecast(
            pressures,
            clouds,
            forecastClimate,
            hourlyForecastChangeThreshold.absoluteValue / 3f,
            stormThreshold.absoluteValue / 3f,
            now,
            location
        )
    }

    private fun getTendency(forecast: List<WeatherForecast>): PressureTendency {
        return forecast.firstOrNull()?.get3hTendency() ?: PressureTendency.zero
    }

    private suspend fun getHighLowTemperature(
        startTime: ZonedDateTime,
        start: Duration,
        duration: Duration
    ): Range<Temperature> {
        return temperatureService.getTemperatureRange(
            startTime.plus(start),
            startTime.plus(start).plus(duration)
        )
    }

    companion object {
        private val minDuration = Duration.ofMinutes(10)
        private val SKY_CONDITIONS = setOf(WeatherCondition.Clear, WeatherCondition.Overcast)
        private val WEATHER_CONDITIONS = setOf(
            WeatherCondition.Precipitation,
            WeatherCondition.Storm,
            WeatherCondition.Wind,
            WeatherCondition.Thunderstorm
        )
    }
}
