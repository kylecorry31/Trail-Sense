package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import android.util.Size
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.GeographicImageUtils
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader

object LandModel {

    // Cache
    private val locationToWaterPixelCache = LRUCache<Coordinate, PixelCoordinate?>(size = 20)

    // Image data source
    private val size = Size(3800, 1900)
    private val searchSize = 100
    private val precision = 4

    private val source = GeographicImageSource(
        EncodedDataImageReader(
            SingleImageReader(size, AssetInputStreamable("land.webp")),
            decoder = EncodedDataImageReader.scaledDecoder(1.0, 0.0, false),
            maxChannels = 3
        ),
        precision = precision,
        interpolationOrder = 0
    )

    suspend fun getNearestWater(
        context: Context,
        location: Coordinate
    ): Coordinate? = onIO {
        val pixel = locationToWaterPixelCache.getOrPut(
            Coordinate(
                location.latitude.roundPlaces(precision),
                location.longitude.roundPlaces(precision)
            )
        ) {
            GeographicImageUtils.getNearestPixelOfAsset(
                source,
                context,
                location,
                "land.webp",
                searchSize,
                hasValue = { isWater(it) },
                hasMappedValue = { isWater(it) }
            )
        } ?: return@onIO null

        source.getLocation(pixel)
    }

    private fun isWater(color: Int): Boolean {
        return color.red == 0 && color.green == 0 && color.blue == 0
    }

    private fun isWater(values: FloatArray): Boolean {
        return values.size >= 3 && SolMath.isZero(values[0]) && SolMath.isZero(values[1]) && SolMath.isZero(
            values[2]
        )
    }
}
