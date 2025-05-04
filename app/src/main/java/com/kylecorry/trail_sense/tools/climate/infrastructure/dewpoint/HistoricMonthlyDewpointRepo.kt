package com.kylecorry.trail_sense.tools.climate.infrastructure.dewpoint

import android.content.Context
import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import java.time.Month

internal object HistoricMonthlyDewpointRepo {

    // Cache
    private val cache = LRUCache<PixelCoordinate, Map<Month, Temperature>>(size = 5)

    // Image data source
    private val size = Size(360, 180)

    private val source = GeographicImageSource(
        size,
        interpolate = true,
        decoder = GeographicImageSource.scaledDecoder(2.7882964611053467, 62.11231994628906)
    )

    private val extensionMap = mapOf(
        "1-3" to Triple(Month.JANUARY, Month.FEBRUARY, Month.MARCH),
        "4-6" to Triple(Month.APRIL, Month.MAY, Month.JUNE),
        "7-9" to Triple(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
        "10-12" to Triple(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER)
    )

    suspend fun getMonthlyDewpoint(
        context: Context,
        location: Coordinate
    ): Map<Month, Temperature> = onIO {
        val pixel = source.getPixel(location)

        cache.getOrPut(pixel) {
            load(context, location)
        }
    }

    private suspend fun load(
        context: Context,
        location: Coordinate
    ): Map<Month, Temperature> = onIO {
        val loaded = mutableMapOf<Month, Temperature>()

        for ((extension, months) in extensionMap) {
            val file = "dewpoint/dewpoint-${extension}.webp"
            val data = source.read(context, file, location)
            loaded[months.first] = Temperature.celsius(data[0])
            loaded[months.second] = Temperature.celsius(data[1])
            loaded[months.third] = Temperature.celsius(data[2])
        }

        loaded
    }


}