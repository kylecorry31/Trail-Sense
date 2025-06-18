package com.kylecorry.trail_sense.shared

import android.content.Context
import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.GeographicImageSource

object AltitudeCorrection {

    // Cache
    private var cache = LRUCache<PixelCoordinate, Float>(size = 5)

    // Image data source
    private const val a = 1.3350785
    private const val b = 106.0
    private const val file = "geoids.webp"
    private val source = GeographicImageSource(
        Size(361, 181),
        decoder = GeographicImageSource.scaledDecoder(a, b),
        interpolationOrder = 2
    )

    suspend fun getGeoid(context: Context, location: Coordinate): Float = onIO {
        val pixel = source.getPixel(location)
        cache.getOrPut(pixel) {
            source.read(context, file, location).first()
        }
    }
}