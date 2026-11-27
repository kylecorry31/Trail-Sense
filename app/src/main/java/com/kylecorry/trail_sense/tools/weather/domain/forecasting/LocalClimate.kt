package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.ForecastClimate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import java.time.Instant
import java.time.Month
import java.time.ZoneOffset

internal data class LocalClimate(
    val monthlyPrecipitation: Map<Month, Distance>,
    val dailyTemperatures: Range<Temperature>
) {

    fun getForecastClimate(time: Instant, forecastTemperatures: Range<Temperature>? = null): ForecastClimate {
        return ForecastClimate(forecastTemperatures, monthlyPrecipitation[time.atZone(ZoneOffset.UTC).month])
    }
}
