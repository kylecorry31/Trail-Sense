package com.kylecorry.trail_sense.weather.domain.forecasting.arrival

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.HourlyArrivalTime
import com.kylecorry.trail_sense.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.weather.domain.get3hTendency
import com.kylecorry.trail_sense.weather.domain.getLastCloud
import java.time.Duration
import java.time.Instant

internal class WeatherArrivalTimeCalculator : IWeatherArrivalTimeCalculator {
    override fun getArrivalTime(
        forecast: List<WeatherForecast>,
        clouds: List<Reading<CloudGenus?>>
    ): HourlyArrivalTime? {

        val timeUntilForecast = forecast.first().time?.let { Duration.between(Instant.now(), it) }
        // TODO: Use time until forecast if available

        val lastCloud = clouds.getLastCloud(Duration.ofHours(4))
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

        return when {
            primaryCondition == null -> null
            steadySystem || stormCloudsSeen -> HourlyArrivalTime.Now
            currentConditions.contains(WeatherCondition.Storm) || tendency.characteristic.isRapid -> HourlyArrivalTime.VerySoon
            tendency.characteristic != PressureCharacteristic.Steady -> HourlyArrivalTime.Soon
            else -> HourlyArrivalTime.Later
        }
    }

    /**
     * Gets the 3h pressure tendency
     */
    private fun getTendency(forecast: List<WeatherForecast>): PressureTendency {
        return forecast.firstOrNull()?.get3hTendency() ?: PressureTendency.zero
    }
}