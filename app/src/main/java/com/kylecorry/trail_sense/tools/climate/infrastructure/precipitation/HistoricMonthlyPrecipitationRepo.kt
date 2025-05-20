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

internal object HistoricMonthlyPrecipitationRepo {

    // Cache
    private val cache = LRUCache<PixelCoordinate, Map<Month, Distance>>(size = 5)

    // Image data source
    private val size = Size(360, 180)

    private val source = GeographicImageSource(
        size,
        interpolate = false,
        decoder = GeographicImageSource.split16BitDecoder()
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

        for (month in Month.entries) {
            val file = "precipitation/precipitation-${month.value}.webp"
            val data = source.read(context, file, location)
            loaded[month] = data.first() / 30f
        }

        loaded
    }


}