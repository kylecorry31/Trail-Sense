package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import androidx.annotation.RawRes
import com.kylecorry.andromeda.compression.CompressionUtils
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.time.Month
import kotlin.math.roundToInt

// TODO: Add a cache
internal object HistoricMonthlyTemperatureRangeRepo {

    internal const val lonStep = 2
    internal const val minLat = -60
    internal const val maxLat = 84

    suspend fun getMonthlyTemperatureRange(
        context: Context,
        location: Coordinate,
        month: Month
    ): Range<Temperature> = onIO {
        val lat = location.latitude.roundToInt().coerceIn(minLat, maxLat)
        val lon = location.longitude.roundToInt()
        val low = loadMinimum(context, lat, lon, month)
        val high = loadMaximum(context, lat, lon, month)
        Range(
            Temperature(low?.toFloat() ?: 0f, TemperatureUnits.F).celsius(),
            Temperature(high?.toFloat() ?: 0f, TemperatureUnits.F).celsius()
        )
    }

    private fun loadMinimum(context: Context, latitude: Int, longitude: Int, month: Month): Byte? {
        return loadMonthly(context, latitude, longitude, month, R.raw.low_temperatures_global)
    }

    private fun loadMaximum(context: Context, latitude: Int, longitude: Int, month: Month): Byte? {
        return loadMonthly(context, latitude, longitude, month, R.raw.high_temperatures_global)
    }

    private fun loadMonthly(
        context: Context,
        latitude: Int,
        longitude: Int,
        month: Month,
        @RawRes file: Int
    ): Byte? {
        val input = context.resources.openRawResource(file)
        val lonIdx = (longitude + 180) / lonStep + 1
        val latitudes = maxLat - minLat + 1

        val valuesPerLat = 360 / lonStep + 1
        val valuesPerMonth = latitudes * valuesPerLat
        val monthIdx = month.value - 1

        val latIdx = latitude - minLat

        val line = (monthIdx * valuesPerMonth + latIdx * valuesPerLat + lonIdx)
//        println("$latitude, $longitude, $month, $line, $latIdx, $lonIdx")
        return CompressionUtils.getBytes(input, line, 1)?.get(0)
    }

}