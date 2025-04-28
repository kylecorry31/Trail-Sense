package com.kylecorry.trail_sense.tools.climate.infrastructure.precipitation

import android.content.Context
import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import java.time.Month

object HistoricMonthlyPrecipitationRepo {

    // Cache
    private val cache = LRUCache<PixelCoordinate, Map<Month, Distance>>(size = 5)

    // Image data source
    private const val a = 2.5986549854278564
    private const val b = 0.0
    private const val latitudePixelsPerDegree = 2.0
    private const val longitudePixelsPerDegree = 1.6
    private val size = Size(576, 361)

    private val source = GeographicImageSource(
        size,
        latitudePixelsPerDegree,
        longitudePixelsPerDegree,
        decoder = GeographicImageSource.scaledDecoder(a, b)
    )

    private val extensionMap = mapOf(
        "1-3" to Triple(Month.JANUARY, Month.FEBRUARY, Month.MARCH),
        "4-6" to Triple(Month.APRIL, Month.MAY, Month.JUNE),
        "7-9" to Triple(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
        "10-12" to Triple(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER)
    )

    suspend fun getMonthlyPrecipitation(
        context: Context,
        location: Coordinate
    ): Map<Month, Distance> = onIO {
        val pixel = source.getPixel(location)

        cache.getOrPut(pixel) {
            val values = load(context, location)
            Month.entries.associateWith {
                val daysInMonth = it.length(false)
                Distance((values[it] ?: 0f) * daysInMonth, DistanceUnits.Millimeters).meters()
            }
        }
    }

    private suspend fun load(
        context: Context,
        location: Coordinate
    ): Map<Month, Float> = onIO {
        val loaded = mutableMapOf<Month, Float>()

        for ((extension, months) in extensionMap) {
            val file = "precipitation/precipitation-${extension}.webp"
            val data = source.read(context, file, location)
            loaded[months.first] = data[0]
            loaded[months.second] = data[1]
            loaded[months.third] = data[2]
        }

        loaded
    }


}