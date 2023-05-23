package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import androidx.annotation.RawRes
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.io.InputStream
import java.time.Month
import kotlin.math.roundToInt

// TODO: Add a cache
internal object HistoricMonthlyTemperatureRangeRepo {

    internal const val lonStep = 2
    private const val minLat = -56
    private const val maxLat = 83

    suspend fun getMonthlyTemperatureRange(
        context: Context,
        location: Coordinate,
        month: Month
    ): Range<Temperature> = onIO {
        val lat = location.latitude.roundToInt().coerceIn(minLat, maxLat)
        val lon = location.longitude.roundToInt()
        val lowOffset = 52
        val low = loadMinimum(context, lat, lon, month)
        val delta = loadDelta(context, lat, lon, month)
        Range(
            Temperature((low?.toFloat() ?: 0f) - lowOffset, TemperatureUnits.C),
            Temperature((low?.toFloat() ?: 0f) + (delta?.toFloat() ?: 0f) - lowOffset, TemperatureUnits.C)
        )
    }

    private fun loadMinimum(context: Context, latitude: Int, longitude: Int, month: Month): Byte? {
        return loadMonthly(context, latitude, longitude, month, R.raw.tmn, 7)
    }

    private fun loadDelta(context: Context, latitude: Int, longitude: Int, month: Month): Byte? {
        return loadMonthly(context, latitude, longitude, month, R.raw.tdelta, 5)
    }

    private fun loadMonthly(
        context: Context,
        latitude: Int,
        longitude: Int,
        month: Month,
        @RawRes file: Int,
        bits: Int = 8
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
        return getValue(input, line, bits)
    }

    private fun getValue(input: InputStream, line: Int, bits: Int): Byte? {
        return CompressionUtils.getByte(input, line, bits)
    }


}