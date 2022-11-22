package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import androidx.annotation.RawRes
import com.kylecorry.andromeda.compression.CompressionUtils
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import java.time.Month
import kotlin.math.roundToInt

internal object HistoricTemperatureLookup {

    fun getMonthlyTemperatureRange(
        context: Context,
        location: Coordinate,
        month: Month
    ): Range<Temperature> {
        val loc = location.latitude.roundToInt()
        val low = loadMinimum(context, loc, month)
        val high = loadMaximum(context, loc, month)
        return Range(
            Temperature(low?.toFloat() ?: 0f, TemperatureUnits.F).celsius(),
            Temperature(high?.toFloat() ?: 0f, TemperatureUnits.F).celsius()
        )
    }

    private fun loadMinimum(context: Context, latitude: Int, month: Month): Short? {
        return loadMonthly(context, latitude, month, R.raw.low_temperatures)
    }

    private fun loadMaximum(context: Context, latitude: Int, month: Month): Short? {
        return loadMonthly(context, latitude, month, R.raw.high_temperatures)
    }

    private fun loadMonthly(
        context: Context,
        latitude: Int,
        month: Month,
        @RawRes file: Int
    ): Short? {
        val input = context.resources.openRawResource(file)
        val line = ((month.value - 1) * 181 + (90 + latitude))
        return CompressionUtils.getShort(input, line)
    }

}