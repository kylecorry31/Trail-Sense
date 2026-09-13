package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import java.time.ZonedDateTime

private const val TAG = "TemperaturePrediction"

// Lookups repeat on every weather calculation, so only log the first failure until one succeeds
@Volatile
private var isLookupFailing = false

internal suspend fun ITemperatureService.getTemperaturePrediction(time: ZonedDateTime): TemperaturePrediction? {
    return try {
        val range = getTemperatureRange(time.toLocalDate())
        val low = range.start
        val high = range.end
        val current = getTemperature(time)
        val average = Temperature.from((low.value + high.value) / 2f, low.units)
        isLookupFailing = false
        TemperaturePrediction(
            average,
            low,
            high,
            current
        )
    } catch (e: Exception) {
        if (!isLookupFailing) {
            isLookupFailing = true
            getAppService<Logger>().warn(TAG, "Unable to lookup temperature", e)
        }
        null
    }
}
