package com.kylecorry.trail_sense.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.PressureTendency
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
        val mapped = readings.map { it.pressureReading() }
        // Gets the weather reading twice - first to get arrival time, second to determine precipitation type
        val original = getForecast(
            mapped,
            clouds,
            null
        )

        val arrival = arrivalCalculator.getArrivalTime(original, clouds)

        val arrivesIn = when (arrival) {
            HourlyArrivalTime.Now, null -> Duration.ZERO
            HourlyArrivalTime.VerySoon -> Duration.ofHours(1)
            HourlyArrivalTime.Soon -> Duration.ofHours(2)
            HourlyArrivalTime.Later -> Duration.ofHours(8)
        }

        val temperatures =
            getHighLowTemperature(
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

    private fun getForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        temperatureRange: Range<Temperature>?
    ): List<WeatherForecast> {
        return Meteorology.forecast(
            pressures,
            clouds,
            temperatureRange,
            hourlyForecastChangeThreshold / 3f,
            stormThreshold / 3f
        )
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
}