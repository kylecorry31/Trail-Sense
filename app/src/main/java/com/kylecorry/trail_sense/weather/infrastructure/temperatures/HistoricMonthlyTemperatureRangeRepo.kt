package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
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
        val low = loadMonthly(context, location, month, "tmn") ?: 0f
        val high = loadMonthly(context, location, month, "tmx") ?: 0f
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
    ): Float? {
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

        val decoder = BitmapRegionDecoder.newInstance(fileSystem.stream(file), false) ?: return null
        val bitmap = decoder.decodeRegion(getRegion(x, y), BitmapFactory.Options())

        var sum = 0
        var count = 0
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val pixel = when (channel) {
                    0 -> bitmap[i, j].red
                    1 -> bitmap[i, j].green
                    2 -> bitmap[i, j].blue
                    else -> return null
                }

                if (pixel > 0) {
                    sum += pixel
                    count++
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

    private fun getRegion(x: Int, y: Int): Rect {
        // TODO: Verify dimension
        // X and Y are at the center of the square with length "size"
        val width = 720
        val height = 360

        val left = x// - 1
        val top = y //- 1
        val bottom = y + 1// + 2
        val right = x + 1// + 2

        return Rect(
            left.coerceIn(0, width),
            top.coerceIn(0, height),
            right.coerceIn(0, width),
            bottom.coerceIn(0, height)
        )

    }


}