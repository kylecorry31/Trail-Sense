package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import android.util.Size
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.getChannel
import com.kylecorry.andromeda.core.bitmap.ColorChannel
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.ImageDataSource
import java.time.Month

internal object HistoricMonthlyTemperatureRangeRepo {

    private val imageDataSource = ImageDataSource(
        Size(720, 360),
        3,
        2
    ) { it.red > 0 }

    private const val pixelsPerDegree = 2

    private var cachedPixel: Pair<Int, Int>? = null
    private var cachedData: Map<Month, Range<Temperature>>? = null

    suspend fun getMonthlyTemperatureRange(
        context: Context,
        location: Coordinate,
        month: Month
    ): Range<Temperature> = onIO {
        getMonthlyTemperatureRanges(context, location)[month] ?: Range(
            Temperature.zero,
            Temperature.zero
        )
    }

    suspend fun getMonthlyTemperatureRanges(
        context: Context,
        location: Coordinate
    ): Map<Month, Range<Temperature>> = onIO {
        // TODO: Pixel contains 3 months, use that
        val pixel = getPixel(location)

        if (cachedPixel != pixel) {
            cachedPixel = pixel
            cachedData = Month.values().associateWith {
                val lowOffset = 61
                val highOffset = 48
                val low = loadMonthly(context, pixel, it, "tmn")
                val high = loadMonthly(context, pixel, it, "tmx")
                Range(
                    Temperature(low - lowOffset, TemperatureUnits.F).celsius(),
                    Temperature(high - highOffset, TemperatureUnits.F).celsius()
                )
            }
        }

        cachedData!!
    }

    private fun getPixel(location: Coordinate): Pair<Int, Int> {
        val x = ((location.longitude + 180) * pixelsPerDegree).toInt() - 1
        val y = ((180 - (location.latitude + 90)) * pixelsPerDegree).toInt() - 1
        return x to y
    }

    private suspend fun loadMonthly(
        context: Context,
        pixel: Pair<Int, Int>,
        month: Month,
        type: String
    ): Float {
        val fileSystem = AssetFileSystem(context)
        val monthRange = when (month) {
            in Month.JANUARY..Month.MARCH -> "1-3"
            in Month.APRIL..Month.JUNE -> "4-6"
            in Month.JULY..Month.SEPTEMBER -> "7-9"
            else -> "10-12"
        }

        val channel = when (month) {
            Month.JANUARY, Month.APRIL, Month.JULY, Month.OCTOBER -> ColorChannel.Red
            Month.FEBRUARY, Month.MAY, Month.AUGUST, Month.NOVEMBER -> ColorChannel.Green
            Month.MARCH, Month.JUNE, Month.SEPTEMBER, Month.DECEMBER -> ColorChannel.Blue
        }

        val file = "temperatures/$type-$monthRange.webp"

        val value =
            imageDataSource.getPixel(fileSystem.stream(file), pixel.first, pixel.second, true)
                ?: return 0f

        return value.getChannel(channel).toFloat()
    }

}