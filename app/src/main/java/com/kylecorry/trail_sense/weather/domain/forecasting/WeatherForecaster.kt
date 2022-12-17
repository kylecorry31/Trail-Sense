package com.kylecorry.trail_sense.weather.domain.forecasting

import android.util.Log
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.*
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

internal class WeatherForecaster(
    private val temperatureService: ITemperatureService,
    prefs: IWeatherPreferences
) : IWeatherForecaster {

    private val stormThreshold = prefs.stormAlertThreshold
    private val hourlyForecastChangeThreshold = prefs.hourlyForecastChangeThreshold

    override suspend fun forecast(
        observations: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>
    ): CurrentWeather {
        val forecast = getForecast(observations, clouds)
        val last = observations.lastOrNull()
        val lastCloud = getLastCloud(clouds)

        val tendency = getTendency(forecast.second)

        val historicalTemperature = try {
            val range = temperatureService.getTemperatureRange(LocalDate.now())
            val low = range.start
            val high = range.end
            val current = temperatureService.getTemperature(ZonedDateTime.now())
            val average = Temperature((low.temperature + high.temperature) / 2f, low.units)
            TemperaturePrediction(average, low, high, current)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Unable to lookup average temperature", e)
            null
        }

        return CurrentWeather(
            WeatherPrediction(
                forecast.second.first().conditions,
                forecast.second.last().conditions,
                forecast.second.first().front,
                forecast.first,
                historicalTemperature,
                getWeatherAlerts(forecast.second.first().conditions) +
                        getTemperatureAlerts(historicalTemperature)
            ),
            tendency.copy(amount = tendency.amount * 3),
            last,
            lastCloud
        )
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

        val arrival = getArrivalTime(original, clouds)

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

    private fun getLastCloud(clouds: List<Reading<CloudGenus?>>): Reading<CloudGenus?>? {
        var lastCloud = clouds.lastOrNull()
        val maxCloudTime = Duration.ofHours(4)
        if (lastCloud == null || Duration.between(lastCloud.time, Instant.now())
                .abs() > maxCloudTime
        ) {
            lastCloud = null
        }
        return lastCloud
    }

    private fun getArrivalTime(
        forecast: List<WeatherForecast>,
        clouds: List<Reading<CloudGenus?>>
    ): HourlyArrivalTime? {
        val lastCloud = getLastCloud(clouds)
        val tendency = getTendency(forecast)
        val stormCloudsSeen = listOf<CloudGenus?>(
            CloudGenus.Cumulonimbus,
            CloudGenus.Nimbostratus
        ).contains(lastCloud?.value)

        val currentConditions = forecast.first().conditions
        val primaryCondition = WeatherPrediction(
            currentConditions,
            emptyList(),
            null,
            HourlyArrivalTime.Now,
            null,
            emptyList()
        ).primaryHourly

        val steadySystem =
            tendency.characteristic == PressureCharacteristic.Steady && listOf(
                WeatherCondition.Clear,
                WeatherCondition.Overcast
            ).contains(primaryCondition)

        // TODO: Replace with hourly forecast
        return when {
            primaryCondition == null -> null
            steadySystem || stormCloudsSeen -> HourlyArrivalTime.Now
            currentConditions.contains(WeatherCondition.Storm) || tendency.characteristic.isRapid -> HourlyArrivalTime.VerySoon
            tendency.characteristic != PressureCharacteristic.Steady -> HourlyArrivalTime.Soon
            else -> HourlyArrivalTime.Later
        }
    }

    private fun getTendency(forecast: List<WeatherForecast>): PressureTendency {
        return forecast.firstOrNull()?.tendency ?: PressureTendency(
            PressureCharacteristic.Steady,
            0f
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

    private fun getWeatherAlerts(conditions: List<WeatherCondition>): List<WeatherAlert> {
        return if (conditions.contains(WeatherCondition.Storm)) {
            listOf(WeatherAlert.Storm)
        } else {
            emptyList()
        }
    }

    private fun getTemperatureAlerts(temperatures: TemperaturePrediction?): List<WeatherAlert> {
        temperatures ?: return emptyList()
        return if (temperatures.low.celsius().temperature <= WeatherSubsystem.COLD) {
            listOf(WeatherAlert.Cold)
        } else if (temperatures.high.celsius().temperature >= WeatherSubsystem.HOT) {
            listOf(WeatherAlert.Hot)
        } else {
            emptyList()
        }
    }
}