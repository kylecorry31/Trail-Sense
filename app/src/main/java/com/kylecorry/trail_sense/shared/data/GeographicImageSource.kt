package com.kylecorry.trail_sense.shared.data

import android.content.Context
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.bitmap.ImagePixelReader
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.Coordinate
import java.io.InputStream

class GeographicImageSource(
    imageSize: Size,
    private val latitudePixelsPerDegree: Double = 1.0,
    private val longitudePixelsPerDegree: Double = 1.0,
    private val precision: Int = 2,
    interpolate: Boolean = true,
    private val decoder: (Int?) -> List<Float> = { listOf(it?.toFloat() ?: 0f) }
) {

    private val reader = ImagePixelReader(imageSize, interpolate)

    fun getPixel(location: Coordinate): PixelCoordinate {
        val x = (location.longitude + 180) * longitudePixelsPerDegree
        val y = (180 - (location.latitude + 90)) * latitudePixelsPerDegree
        return PixelCoordinate(
            x.roundPlaces(precision).toFloat(),
            y.roundPlaces(precision).toFloat()
        )
    }

    suspend fun read(stream: InputStream, location: Coordinate): List<Float> = onIO {
        val pixel = getPixel(location)
        val data = reader.getPixel(stream, pixel.x, pixel.y, true)
        decoder(data)
    }

    suspend fun read(context: Context, filename: String, location: Coordinate): List<Float> = onIO {
        val fileSystem = AssetFileSystem(context)
        read(fileSystem.stream(filename), location)
    }

    companion object {

        fun scaledDecoder(a: Float, b: Float): (Int?) -> List<Float> {
            return {
                val red = it?.red?.toFloat() ?: 0f
                val green = it?.green?.toFloat() ?: 0f
                val blue = it?.blue?.toFloat() ?: 0f
                val alpha = it?.alpha?.toFloat() ?: 0f
                listOf(red / a - b, green / a - b, blue / a - b, alpha / a - b)
            }
        }

        fun offsetDecoder(offset: Float): (Int?) -> List<Float> {
            return {
                val red = it?.red?.toFloat() ?: 0f
                val green = it?.green?.toFloat() ?: 0f
                val blue = it?.blue?.toFloat() ?: 0f
                val alpha = it?.alpha?.toFloat() ?: 0f
                listOf(red - offset, green - offset, blue - offset, alpha - offset)
            }
        }
    }

}