package com.kylecorry.trail_sense.shared

import android.content.Context
import android.util.Size
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.io.ImageDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 *  The geoids.csv is from https://github.com/vectorstofinal/geoid_heights licensed under MIT
 */
object AltitudeCorrection {

    // Cache
    private var cachedPixel: PixelCoordinate? = null
    private var cachedData: Float? = null
    private var mutex = Mutex()

    // Image data source
    private const val latitudePixelsPerDegree = 1.0
    private const val longitudePixelsPerDegree = 1.0
    private val imageDataSource = ImageDataSource(
        Size(361, 181),
        3,
        ImageDataSource.geographicSampler()
    )
    private const val offset = 106

    suspend fun getGeoid(context: Context, location: Coordinate): Float = onIO {
        mutex.withLock {
            val fileSystem = AssetFileSystem(context)
            val file = "geoids.webp"
            val pixel = getPixel(location)
            if (pixel == cachedPixel) {
                return@onIO cachedData ?: 0f
            }

            val data =
                imageDataSource.getPixel(fileSystem.stream(file), pixel, true)
                    ?: return@onIO 0f

            cachedPixel = pixel
            cachedData = data.red.toFloat() - offset
            cachedData!!
        }
    }

    private fun getPixel(location: Coordinate): PixelCoordinate {
        val places = 2
        val x = (location.longitude + 180) * longitudePixelsPerDegree
        val y = (180 - (location.latitude + 90)) * latitudePixelsPerDegree
        return PixelCoordinate(x.roundPlaces(places).toFloat(), y.roundPlaces(places).toFloat())
    }


}