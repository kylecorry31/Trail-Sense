package com.kylecorry.trail_sense.weather.infrastructure.temperatures

import android.content.Context
import android.util.Size
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.ImageDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Month

internal object HistoricMonthlyTemperatureRangeRepo {

    // Cache
    private var cachedPixel: Pair<Int, Int>? = null
    private var cachedData: Map<Month, Range<Temperature>>? = null
    private var mutex = Mutex()

    // Image data source
    private val imageDataSource = ImageDataSource(
        Size(720, 360),
        3,
        2
    ) { it.red > 0 }
    private const val latitudePixelsPerDegree = 2.0
    private const val longitudePixelsPerDegree = 2.0
    private const val lowOffset = 61
    private const val highOffset = 48
    private val extensionMap = mapOf(
        "1-3" to Triple(Month.JANUARY, Month.FEBRUARY, Month.MARCH),
        "4-6" to Triple(Month.APRIL, Month.MAY, Month.JUNE),
        "7-9" to Triple(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
        "10-12" to Triple(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER)
    )
    private val lowType = "tmn"
    private val highType = "tmx"

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
        mutex.withLock {
            val pixel = getPixel(location)

            if (cachedPixel != pixel) {
                cachedPixel = pixel
                val lows = load(context, pixel, lowType) ?: emptyMap()
                val highs = load(context, pixel, highType) ?: emptyMap()

                val allZeros = lows.values.all { it == 0 } && highs.values.all { it == 0 }

                // TODO: If values are all zeros, estimate the temperature range based on the latitude and month
                cachedData = Month.values().associateWith {
                    val low = if (allZeros) 32f else (lows[it] ?: 0) - lowOffset
                    val high = if (allZeros) 33f else (highs[it] ?: 0) - highOffset
                    Range(
                        Temperature(low.toFloat(), TemperatureUnits.F).celsius(),
                        Temperature(high.toFloat(), TemperatureUnits.F).celsius()
                    )
                }
            }

            cachedData!!
        }
    }

    private fun getPixel(location: Coordinate): Pair<Int, Int> {
        val x = ((location.longitude + 180) * longitudePixelsPerDegree).toInt() - 1
        val y = ((180 - (location.latitude + 90)) * latitudePixelsPerDegree).toInt() - 1
        return x to y
    }

    private suspend fun load(
        context: Context,
        pixel: Pair<Int, Int>,
        type: String
    ): Map<Month, Int>? = onIO {
        val fileSystem = AssetFileSystem(context)

        val loaded = mutableMapOf<Month, Int>()

        for ((extension, months) in extensionMap) {
            val file = "temperatures/$type-${extension}.webp"
            val data =
                imageDataSource.getPixel(fileSystem.stream(file), pixel.first, pixel.second, true)
                    ?: return@onIO null
            loaded[months.first] = data.red
            loaded[months.second] = data.green
            loaded[months.third] = data.blue
        }

        loaded
    }

}