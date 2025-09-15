package com.kylecorry.trail_sense.tools.climate.infrastructure.precipitation

import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import java.time.Month

internal object HistoricMonthlyPrecipitationRepo {

    // Cache
    private val cache = LRUCache<PixelCoordinate, Map<Month, Distance>>(size = 5)

    // Image data source
    private val size = Size(360, 180)

    private val sources = Month.entries.associateWith { month ->
        val file = "precipitation/precipitation-${month.value}.webp"
        GeographicImageSource(
            EncodedDataImageReader(
                SingleImageReader(size, AssetInputStreamable(file)),
                decoder = EncodedDataImageReader.split16BitDecoder(),
                maxChannels = 1
            ),
            interpolationOrder = 0
        )
    }

    suspend fun getMonthlyPrecipitation(
        location: Coordinate
    ): Map<Month, Distance> = onIO {
        val pixel = sources[Month.JANUARY]!!.getPixel(location)

        cache.getOrPut(pixel) {
            val values = load(location)
            Month.entries.associateWith {
                val daysInMonth = it.length(false)
                Distance.from((values[it] ?: 0f) * daysInMonth, DistanceUnits.Millimeters).meters()
            }
        }
    }

    private suspend fun load(
        location: Coordinate
    ): Map<Month, Float> = onIO {
        val loaded = mutableMapOf<Month, Float>()

        sources.forEach { (month, source) ->
            val data = source.read(location)
            loaded[month] = data.first() / 30f
        }

        loaded
    }


}