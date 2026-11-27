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
import com.kylecorry.trail_sense.tools.weather.domain.PressureTide
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.alerts.WeatherAlertGenerator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTimeCalculator
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
    private val clock: Clock = Clock.systemDefaultZone()
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
        val mapped = if (!hasEnoughReadings(readings)) {
            emptyList()
        } else {
            readings.map { PressureTide.remove(it.pressureReading(), it.location) }
        }

        // WHY: The forecast is computed twice: first for arrival time, then for precipitation type
        val original = getForecast(
            mapped,
            clouds,
            null,
            location
        )

        val arrival = arrivalCalculator.getArrivalTime(original, clouds)
        val arrivesIn = arrival?.let { Duration.between(Instant.now(clock), it.time) } ?: Duration.ZERO

        // WHY: Temperatures only matter for precipitation, so skip fetching them otherwise
        if (original.none { it.conditions.contains(WeatherCondition.Precipitation) }) {
            return arrival to original
        }

        val temperatures = getHighLowTemperature(
            ZonedDateTime.now(clock),
            arrivesIn,
            Duration.ofHours(6)
        )

        return arrival to getForecast(
            mapped,
            clouds,
            temperatures,
            location
        )
    }

    private fun hasEnoughReadings(readings: List<WeatherObservation>): Boolean {
        val times = readings.map { it.time }
        val start = times.minOrNull() ?: return false
        val end = times.maxOrNull() ?: return false
        return Duration.between(start, end) >= minDuration
    }

    private fun getForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        temperatureRange: Range<Temperature>?,
        location: Coordinate? = null
    ): List<WeatherForecast> {
        return Meteorology.forecast(
            pressures,
            clouds,
            temperatureRange,
            hourlyForecastChangeThreshold.absoluteValue / 3f,
            stormThreshold.absoluteValue / 3f,
            Instant.now(clock),
            location ?: Coordinate.zero
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
    }
}
