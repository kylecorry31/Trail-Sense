package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.io.InputStream
import java.time.Month

// TODO: Add a cache
internal object HistoricMonthlyTemperatureRangeRepo {

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
            Month.JANUARY, Month.APRIL, Month.JULY, Month.OCTOBER -> 0
            Month.FEBRUARY, Month.MAY, Month.AUGUST, Month.NOVEMBER -> 1
            Month.MARCH, Month.JUNE, Month.SEPTEMBER, Month.DECEMBER -> 2
        }

        val y = ((180 - (location.latitude + 90)) * 2).toInt() - 1
        val x = ((location.longitude + 180) * 2).toInt() - 1

        val file = "temperatures/$type-$monthRange.webp"

        val bitmap = fileSystem.stream(file).use {
            decodeAssetRegion(it, x, y)
        } ?: return 0f


        var sum = 0
        var count = 0
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val pixel = when (channel) {
                    0 -> bitmap[i, j].red
                    1 -> bitmap[i, j].green
                    2 -> bitmap[i, j].blue
                    else -> return 0f
                }

                if (pixel > 0) {
                    sum += pixel * if (i == x && j == y) 2 else 1 // Weight the current pixel more (it's the center
                    count += if (i == x && j == y) 2 else 1
                }
            }
        }

        bitmap.recycle()


        return if (count == 0) {
            0f
        } else {
            sum / count.toFloat()
        }
    }

    private suspend fun decodeAssetRegion(stream: InputStream, x: Int, y: Int): Bitmap? = onIO {
        val decoder = if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.R) {
            BitmapRegionDecoder.newInstance(stream)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(stream, false)
        } ?: return@onIO null
        decoder.decodeRegion(getRegion(x, y), BitmapFactory.Options())
    }

    private fun getRegion(x: Int, y: Int): Rect {
        // X and Y are at the center of the square with length "size"
        val width = 720
        val height = 360

        val left = x - 1
        val top = y - 1
        val bottom = y + 2
        val right = x + 2

        return Rect(
            left.coerceIn(0, width),
            top.coerceIn(0, height),
            right.coerceIn(0, width),
            bottom.coerceIn(0, height)
        )

    }


}