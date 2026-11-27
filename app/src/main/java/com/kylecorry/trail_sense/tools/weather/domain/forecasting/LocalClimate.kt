package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import java.time.Month

internal data class LocalClimate(
    val monthlyPrecipitation: Map<Month, Distance>,
    val dailyTemperatures: Range<Temperature>
)
