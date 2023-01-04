package com.kylecorry.trail_sense.weather.domain.forecasting.arrival

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.weather.domain.get3hTendency
import java.time.Instant

internal class WeatherArrivalTimeCalculator : IWeatherArrivalTimeCalculator {
    override fun getArrivalTime(
        forecast: List<WeatherForecast>,
        clouds: List<Reading<CloudGenus?>>
    ): WeatherArrivalTime? {

        val forecastedTime = forecast.first().time

        if (forecastedTime != null) {
            return WeatherArrivalTime(forecastedTime, true)
        }

        val tendency = getTendency(forecast)
        val currentConditions = forecast.first().conditions

        return when {
            currentConditions.isEmpty() -> null
            currentConditions.contains(WeatherCondition.Storm) || tendency.characteristic.isRapid -> WeatherArrivalTime.fromRelative(
                Instant.now(),
                RelativeArrivalTime.VerySoon
            )
            tendency.characteristic != PressureCharacteristic.Steady -> WeatherArrivalTime.fromRelative(
                Instant.now(),
                RelativeArrivalTime.Soon
            )
            else -> WeatherArrivalTime.fromRelative(
                Instant.now(),
                RelativeArrivalTime.Later
            )
        }
    }

    /**
     * Gets the 3h pressure tendency
     */
    private fun getTendency(forecast: List<WeatherForecast>): PressureTendency {
        return forecast.firstOrNull()?.get3hTendency() ?: PressureTendency.zero
    }
}