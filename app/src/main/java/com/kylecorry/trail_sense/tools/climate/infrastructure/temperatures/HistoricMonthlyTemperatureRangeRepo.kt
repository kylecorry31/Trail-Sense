package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures

import android.content.Context
import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import java.time.Month
import kotlin.math.max
import kotlin.math.min

internal object HistoricMonthlyTemperatureRangeRepo {

    // Cache
    private val cache = LRUCache<PixelCoordinate, Map<Month, Range<Temperature>>>(size = 5)

    // Image data source
    private const val highA = 1.356383
    private const val highB = 48.0
    private const val lowA = 1.4088398
    private const val lowB = 56.0
    private const val latitudePixelsPerDegree = 2.0
    private const val longitudePixelsPerDegree = 1.6
    private val size = Size(576, 361)

    private val lowSource = GeographicImageSource(
        size,
        latitudePixelsPerDegree = latitudePixelsPerDegree,
        longitudePixelsPerDegree = longitudePixelsPerDegree,
        decoder = GeographicImageSource.scaledDecoder(lowA, lowB)
    )

    private val highSource = GeographicImageSource(
        size,
        latitudePixelsPerDegree = latitudePixelsPerDegree,
        longitudePixelsPerDegree = longitudePixelsPerDegree,
        decoder = GeographicImageSource.scaledDecoder(highA, highB)
    )

    private val extensionMap = mapOf(
        "1-3" to Triple(Month.JANUARY, Month.FEBRUARY, Month.MARCH),
        "4-6" to Triple(Month.APRIL, Month.MAY, Month.JUNE),
        "7-9" to Triple(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
        "10-12" to Triple(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER)
    )
    private const val lowType = "T2MMIN"
    private const val highType = "T2MMAX"

    suspend fun getMonthlyTemperatureRanges(
        context: Context,
        location: Coordinate
    ): Map<Month, Range<Temperature>> = onIO {
        val pixel = lowSource.getPixel(location)

        cache.getOrPut(pixel) {
            val lows = load(context, location, lowType)
            val highs = load(context, location, highType)
            Month.values().associateWith {
                val low = lows[it] ?: 0f
                val high = highs[it] ?: 0f
                Range(
                    Temperature(min(low, high), TemperatureUnits.F).celsius(),
                    Temperature(max(low, high), TemperatureUnits.F).celsius()
                )
            }
        }
    }

    private suspend fun load(
        context: Context,
        location: Coordinate,
        type: String
    ): Map<Month, Float> = onIO {
        val loaded = mutableMapOf<Month, Float>()

        for ((extension, months) in extensionMap) {
            val file = "temperatures/$type-${extension}.webp"
            val source = if (type == lowType) lowSource else highSource
            val data = source.read(context, file, location)
            loaded[months.first] = data[0]
            loaded[months.second] = data[1]
            loaded[months.third] = data[2]
        }

        loaded
    }

}