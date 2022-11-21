package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import com.kylecorry.andromeda.compression.CompressionUtils
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import java.time.Month
import kotlin.math.roundToInt

internal object HistoricTemperatureLookup {

    fun getMonthlyAverageTemperature(
        context: Context,
        location: Coordinate,
        month: Month
    ): Temperature {
        val loc = location.latitude.roundToInt()
        val temperature = loadTemperature(context, loc, month)
        return Temperature(temperature?.toFloat() ?: 0f, TemperatureUnits.F).celsius()
    }

    fun getTemperatureDiurnalRange(context: Context, location: Coordinate): Float {
        val loc = location.latitude.roundToInt()
        val temperature = loadTemperatureRange(context, loc)?.toFloat() ?: 0f
        return temperature * 5 / 9f
    }

    private fun loadTemperatureRange(context: Context, key: Int): Short? {
        val input = context.resources.openRawResource(R.raw.temperature_range)
        val line = 90 + key
        return CompressionUtils.getShort(input, line)
    }

    private fun loadTemperature(context: Context, key: Int, month: Month): Short? {
        val input = context.resources.openRawResource(R.raw.temperatures)
        val line = ((month.value - 1) * 181 + (90 + key))
        return CompressionUtils.getShort(input, line)
    }

}