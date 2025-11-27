package com.kylecorry.trail_sense.shared

import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader

object AltitudeCorrection {

    // Cache
    private var cache = LRUCache<PixelCoordinate, Float>(size = 5)

    // Image data source
    private const val a = 1.3232228755950928
    private const val b = 106.9103012084961
    private const val file = "geoids.webp"
    private val source = GeographicImageSource(
        EncodedDataImageReader(
            SingleImageReader(Size(360, 180), AssetInputStreamable(file)),
            maxChannels = 1,
            decoder = EncodedDataImageReader.scaledDecoder(a, b)
        ),
        interpolationOrder = 2,
        valuePixelOffset = 0.5f
    )

    suspend fun getGeoid(location: Coordinate): Float = onIO {
        val pixel = source.getPixel(location)
        cache.getOrPut(pixel) {
            source.read(location).first()
        }
    }
}