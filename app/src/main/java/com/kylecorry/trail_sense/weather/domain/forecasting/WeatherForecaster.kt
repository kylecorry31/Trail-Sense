package com.kylecorry.trail_sense.weather.domain.forecasting

import com.kylecorry.andromeda.core.rangeOrNull
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.domain.forecasting.alerts.WeatherAlertGenerator
import com.kylecorry.trail_sense.weather.domain.forecasting.arrival.WeatherArrivalTimeCalculator
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

internal class WeatherForecaster(
    private val temperatureService: ITemperatureService,
    prefs: IWeatherPreferences
) : IWeatherForecaster {

    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold
    private val arrivalCalculator = WeatherArrivalTimeCalculator()
    private val alertGenerator = WeatherAlertGenerator()

    override suspend fun forecast(
        observations: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>
    ): CurrentWeather {
        val (arrival, forecast) = getForecast(observations, clouds)
        val last = observations.lastOrNull()
        val lastCloud = clouds.getLastCloud(Duration.ofHours(4))

        val tendency = getTendency(forecast)

        val weather = CurrentWeather(
            WeatherPrediction(
                forecast.first().conditions,
                forecast.last().conditions,
                forecast.first().front,
                arrival,
                temperatureService.getTemperaturePrediction(ZonedDateTime.now()),
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
        clouds: List<Reading<CloudGenus?>>
    ): Pair<HourlyArrivalTime?, List<WeatherForecast>> {
        // If there aren't enough readings, don't use them
        val mapped = if (!hasEnoughReadings(readings)) {
            emptyList()
        } else {
            readings.map { it.pressureReading() }
        }

        // Gets the weather reading twice - first to get arrival time, second to determine precipitation type
        val original = getForecast(
            mapped,
            clouds,
            null
        )

        val arrival = arrivalCalculator.getArrivalTime(original, clouds)
        val arrivesIn = getArrivalTime(arrival)

        // The temperatures are only used when there is precipitation, so short circuit if not needed
        if (original.none { it.conditions.contains(WeatherCondition.Precipitation) }) {
            return arrival to original
        }

        val temperatures = getHighLowTemperature(
            ZonedDateTime.now(),
            arrivesIn,
            Duration.ofHours(6)
        )

        return arrival to getForecast(
            mapped,
            clouds,
            temperatures
        )
    }

    private fun hasEnoughReadings(readings: List<WeatherObservation>): Boolean {
        val range = readings.map { it.time }.rangeOrNull() ?: return false
        return Duration.between(range.lower, range.upper) >= minDuration
    }

    private fun getArrivalTime(arrival: HourlyArrivalTime?): Duration {
        return when (arrival) {
            HourlyArrivalTime.Now, null -> Duration.ZERO
            HourlyArrivalTime.VerySoon -> Duration.ofHours(1)
            HourlyArrivalTime.Soon -> Duration.ofHours(2)
            HourlyArrivalTime.Later -> Duration.ofHours(8)
        }
    }

    private fun getForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        temperatureRange: Range<Temperature>?
    ): List<WeatherForecast> {
        val time = Instant.now()
        val forecast = Meteorology.forecast(
            pressures,
            clouds,
            temperatureRange,
            hourlyForecastChangeThreshold / 3f,
            stormThreshold / 3f,
            time
        )

        // There are current conditions, so just return the forecast
        if (forecast.first().conditions.isNotEmpty()) {
            return forecast
        }

        // Try to figure out what the current conditions are based on past predictions
        var startTime = time.minus(noChangePopulationStep)
        val maxTime = time.minus(noChangeMaxHistory)
        while (startTime.isAfter(maxTime)) {
            val hasReadings =
                pressures.any { it.time <= startTime } || clouds.any { it.time <= startTime }
            if (!hasReadings) {
                return forecast
            }

            val previous = Meteorology.forecast(
                pressures,
                clouds,
                temperatureRange,
                hourlyForecastChangeThreshold / 3f,
                stormThreshold / 3f,
                startTime
            )

            // Get the conditions of the previous prediction, starting with the furthest out prediction
            val conditions =
                previous.reversed().firstOrNull { it.conditions.isNotEmpty() }?.conditions
                    ?: emptyList()

            if (conditions.isNotEmpty()) {
                return forecast.withCurrentConditions(conditions)
            }

            startTime = startTime.minus(noChangePopulationStep)
        }

        return forecast
    }

    private fun List<WeatherForecast>.withCurrentConditions(conditions: List<WeatherCondition>): List<WeatherForecast> {
        return mapIndexed { index, value ->
            if (index == 0) {
                value.copy(conditions = conditions)
            } else {
                value
            }
        }
    }

    /**
     * Gets the 3h pressure tendency
     */
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
        private val noChangePopulationStep = Duration.ofMinutes(10)
        private val noChangeMaxHistory = Duration.ofHours(8)
    }
}