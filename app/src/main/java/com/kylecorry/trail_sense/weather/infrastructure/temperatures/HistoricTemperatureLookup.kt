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

    internal const val lonStep = 18

    fun getMonthlyTemperatureRange(
        context: Context,
        location: Coordinate,
        month: Month
    ): Range<Temperature> {
        val lat = location.latitude.roundToInt()
        val lon = location.longitude.roundToInt()
        val low = loadMinimum(context, lat, lon, month)
        val high = loadMaximum(context, lat, lon, month)
        return Range(
            Temperature(low?.toFloat() ?: 0f, TemperatureUnits.F).celsius(),
            Temperature(high?.toFloat() ?: 0f, TemperatureUnits.F).celsius()
        )
    }

    private fun loadMinimum(context: Context, latitude: Int, longitude: Int, month: Month): Short? {
        return loadMonthly(context, latitude, longitude, month, R.raw.low_temperatures_global)
    }

    private fun loadMaximum(context: Context, latitude: Int, longitude: Int, month: Month): Short? {
        return loadMonthly(context, latitude, longitude, month, R.raw.high_temperatures_global)
    }

    private fun loadMonthly(
        context: Context,
        latitude: Int,
        longitude: Int,
        month: Month,
        @RawRes file: Int
    ): Short? {
        val input = context.resources.openRawResource(file)
        val lonIdx = (longitude + 180) / lonStep

        val valuesPerLat = 360 / lonStep + 1
        val valuesPerMonth = 181 * valuesPerLat
        val monthIdx = month.value - 1

        val latIdx = 90 + latitude

        val line = (monthIdx * valuesPerMonth + latIdx * valuesPerLat + lonIdx)
        return CompressionUtils.getShort(input, line)
    }

}