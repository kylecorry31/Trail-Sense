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

        for (month in Month.entries) {
            val file = "dewpoint/dewpoint-${month.value}.webp"
            val data = source.read(context, file, location)
            loaded[month] = Temperature.celsius(data.first())
        }

        loaded
    }


}