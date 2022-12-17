package com.kylecorry.trail_sense.weather.domain.forecasting.arrival

import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.weather.domain.HourlyArrivalTime

interface IWeatherArrivalTimeCalculator {
    fun getArrivalTime(
        forecast: List<WeatherForecast>,
        clouds: List<Reading<CloudGenus?>>
    ): HourlyArrivalTime?
}