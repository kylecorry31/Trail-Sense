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

// TODO: Add a cache
internal object HistoricMonthlyTemperatureRangeRepo {

    private val imageDataSource = ImageDataSource(
        Size(720, 360),
        3,
        2
    ) { it.red > 0 }

    suspend fun getMonthlyTemperatureRange(
        context: Context,
        location: Coordinate,
        month: Month
    ): Range<Temperature> = onIO {
        val lowOffset = 61
        val highOffset = 48
        val low = loadMonthly(context, location, month, "tmn")
        val high = loadMonthly(context, location, month, "tmx")
        Range(
            Temperature(low - lowOffset, TemperatureUnits.F).celsius(),
            Temperature(high - highOffset, TemperatureUnits.F).celsius()
        )
    }

    private suspend fun loadMonthly(
        context: Context,
        location: Coordinate,
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

        val y = ((180 - (location.latitude + 90)) * 2).toInt() - 1
        val x = ((location.longitude + 180) * 2).toInt() - 1

        val file = "temperatures/$type-$monthRange.webp"

        val pixel = imageDataSource.getPixel(fileSystem.stream(file), x, y, true) ?: return 0f

        return pixel.getChannel(channel).toFloat()
    }

}