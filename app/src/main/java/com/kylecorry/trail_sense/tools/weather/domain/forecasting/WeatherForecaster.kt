package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
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
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.alerts.WeatherAlertGenerator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTimeCalculator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.model.WeatherModel
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.get3hTendency
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
    private val arrivalCalculator = WeatherArrivalTimeCalculator(clock)
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
                forecast.laterConditions.map { ForecastCondition(it) },
                forecast.pressure.front,
                forecast.arrival,
                temperatureService.getTemperaturePrediction(ZonedDateTime.now(clock)),
                emptyList()
            ),
            forecast.pressure.get3hTendency() ?: PressureTendency.zero,
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
    ): Forecast {
        val pressures = if (!hasEnoughReadings(readings)) {
            emptyList()
        } else {
            readings.map { removeTide(it.pressureReading(), it.location) }
        }

        val pressureForecast = getSolForecast(pressures, clouds, location)
        val cloudForecast = if (clouds.isEmpty()) null else getSolForecast(emptyList(), clouds, location)
        val pressureConditions = getPressureConditions(pressures, pressureForecast, location)
        val conditions = combine(pressureConditions.first, cloudForecast?.first()?.conditions.orEmpty())
        val laterConditions = combineLater(pressureConditions.second, cloudForecast?.last()?.conditions.orEmpty())
        val primary = primarySelector.getWeather(conditions.map { it.condition })
        val timedWeather = conditions.filter { it.condition in WEATHER_CONDITIONS }.mapNotNull { it.arrival }
        val arrival = arrivalCalculator.getArrivalTime(
            getCloudPrecipitationTime(conditions, cloudForecast?.first()),
            conditions.firstOrNull { it.condition == primary }?.arrival ?: timedWeather.minOrNull()
        )
        val forecast = Forecast(pressureForecast.first(), conditions, laterConditions, arrival)

        val precipitation = conditions.firstOrNull { it.condition == WeatherCondition.Precipitation }
        val now = ZonedDateTime.now(clock)
        val typedPrecipitation = precipitation?.let {
            val arrivesIn = arrival?.let { Duration.between(now.toInstant(), it.time) } ?: Duration.ZERO
            it.copy(condition = getPrecipitationType(getHighLowTemperature(now, arrivesIn, Duration.ofHours(6))))
        }
        val laterType = if (WeatherCondition.Precipitation in laterConditions) {
            getPrecipitationType(getHighLowTemperature(now, LATER_START, LATER_DURATION))
        } else {
            null
        }
        return forecast.copy(
            conditions = conditions + listOfNotNull(typedPrecipitation),
            laterConditions = laterConditions + listOfNotNull(laterType)
        )
    }

    /**
     * @return the conditions over the next 12 hours and the conditions 12 to 24 hours out
     */
    private fun getPressureConditions(
        pressures: List<Reading<Pressure>>,
        solForecast: List<WeatherForecast>,
        location: Coordinate
    ): Pair<List<ForecastCondition>, List<WeatherCondition>> {
        if (climate == null) {
            return solForecast.first().conditions.map { ForecastCondition(it) } to solForecast.last().conditions
        }
        if (!isRecent(pressures.lastOrNull())) {
            return emptyList<ForecastCondition>() to emptyList()
        }
        val prediction = WeatherModel(climate, location, forecastSensitivity, stormSensitivity).predict(pressures)
        return prediction?.conditions.orEmpty() to prediction?.laterConditions.orEmpty()
    }

    private fun isRecent(reading: Reading<Pressure>?): Boolean {
        reading ?: return false
        val maxAge = maxOf(MIN_MAX_READING_AGE, updateFrequency.multipliedBy(2))
        return Duration.between(reading.time, Instant.now(clock)) <= maxAge
    }

    private fun combine(
        pressureConditions: List<ForecastCondition>,
        cloudConditions: List<WeatherCondition>
    ): List<ForecastCondition> {
        val cloudSky = cloudConditions.filter { it in SKY_CONDITIONS }
            .map { ForecastCondition(it, arrival = RelativeArrivalTime.Soon) }
        val sky = cloudSky.ifEmpty { pressureConditions.filter { it.condition in SKY_CONDITIONS } }
        val pressureWeather = pressureConditions.filter { it.condition in WEATHER_CONDITIONS }
        val cloudWeather = cloudConditions
            .filter { it in WEATHER_CONDITIONS && pressureWeather.none { weather -> weather.condition == it } }
            .map { ForecastCondition(it, arrival = RelativeArrivalTime.Soon) }
        val weather = pressureWeather + cloudWeather
        val precipitation = weather.firstOrNull { it.condition == WeatherCondition.Precipitation }
            ?: return sky + weather
        return weather + ForecastCondition(WeatherCondition.Overcast, arrival = precipitation.arrival)
    }

    private fun combineLater(
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

    private fun getPrecipitationType(temperatures: Range<Temperature>): WeatherCondition {
        val mean = (temperatures.start.celsius().value + temperatures.end.celsius().value) / 2
        return if (mean <= SNOW_MEAN_TEMPERATURE_C) WeatherCondition.Snow else WeatherCondition.Rain
    }

    private fun getCloudPrecipitationTime(
        conditions: List<ForecastCondition>,
        cloudForecast: WeatherForecast?
    ): Instant? {
        if (conditions.none { it.condition == WeatherCondition.Precipitation }) {
            return null
        }
        return cloudForecast?.time?.takeIf { WeatherCondition.Precipitation in cloudForecast.conditions }
    }

    private fun removeTide(reading: Reading<Pressure>, location: Coordinate): Reading<Pressure> {
        val tide = Meteorology.getPressureTide(reading.time, location).hpa().value
        val pressure = Pressure.hpa(reading.value.hpa().value - tide).convertTo(reading.value.units)
        return reading.copy(value = pressure)
    }

    private fun hasEnoughReadings(readings: List<WeatherObservation>): Boolean {
        val times = readings.map { it.time }
        val start = times.minOrNull() ?: return false
        val end = times.maxOrNull() ?: return false
        return Duration.between(start, end) >= minDuration
    }

    // WHY: Sol's forecast still provides the pressure tendency, front and cloud conditions, and its thresholds set when the tendency counts as changing
    private fun getSolForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): List<WeatherForecast> {
        return Meteorology.forecast(
            pressures,
            clouds,
            null,
            TENDENCY_CHANGE_HPA_PER_HOUR,
            TENDENCY_STORM_HPA_PER_HOUR,
            Instant.now(clock),
            location
        )
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

    private data class Forecast(
        val pressure: WeatherForecast,
        val conditions: List<ForecastCondition>,
        val laterConditions: List<WeatherCondition>,
        val arrival: WeatherArrivalTime?
    )

    companion object {
        private val minDuration = Duration.ofMinutes(10)
        private val MIN_MAX_READING_AGE = Duration.ofHours(3)
        private val LATER_START = Duration.ofHours(12)
        private val LATER_DURATION = Duration.ofHours(12)
        private const val TENDENCY_CHANGE_HPA_PER_HOUR = 0.5f
        private const val TENDENCY_STORM_HPA_PER_HOUR = 1.5f
        private const val SNOW_MEAN_TEMPERATURE_C = 0f
        private val SKY_CONDITIONS = setOf(WeatherCondition.Clear, WeatherCondition.Overcast)
        private val WEATHER_CONDITIONS = setOf(
            WeatherCondition.Precipitation,
            WeatherCondition.Storm,
            WeatherCondition.Wind,
            WeatherCondition.Thunderstorm
        )
    }
}
