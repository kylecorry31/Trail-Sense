package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.ComplexNumber
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object TideModel {

    // Cache
    private val cache = LRUCache<PixelCoordinate, List<TidalHarmonic>>(size = 5)
    private val locationToPixelCache = LRUCache<Coordinate, PixelCoordinate>(size = 20)

    // Image data source
    private const val a = 0.10273629f
    private const val b = 751.12225f
    private const val latitudePixelsPerDegree = 2.0
    private const val longitudePixelsPerDegree = 2.0
    private val size = Size(721, 361)

    private val source = GeographicImageSource(
        size,
        latitudePixelsPerDegree,
        longitudePixelsPerDegree,
        interpolate = false,
        decoder = GeographicImageSource.scaledDecoder(a, b, false)
    )

    suspend fun getHarmonics(
        context: Context,
        location: Coordinate
    ): List<TidalHarmonic> = onIO {
        val pixel = locationToPixelCache.getOrPut(
            Coordinate(
                location.latitude.roundPlaces(1),
                location.longitude.roundPlaces(1)
            )
        ) {
            getNearestPixel(context, location)
        }

        cache.getOrPut(pixel) {
            load(context, pixel)
        }
    }

    private suspend fun getNearestPixel(context: Context, location: Coordinate): PixelCoordinate {
        val actualPixel = source.getPixel(location)
        val file = "tides/constituents-M2.webp"
        if (source.read(context, file, location)[0] != 0f) {
            return actualPixel
        }

        val fileSystem = AssetFileSystem(context)
        fileSystem.stream(file).use { stream ->
            var bitmap: Bitmap? = null
            try {
                bitmap = BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().also {
                    it.inPreferredConfig = Bitmap.Config.RGB_565
                }) ?: return actualPixel

                // Get the nearest non-zero pixel, wrapping around the image
                val x = actualPixel.x.roundToInt()
                val y = actualPixel.y.roundToInt()
                val width = bitmap.width
                val height = bitmap.height

                // Search in a grid pattern
                for (i in 1..10) {
                    val topY = max(0, y - i)
                    val bottomY = min(height - 1, y + i)
                    val leftX = wrap((x - i).toDouble(), 0.0, (width - 1).toDouble()).toInt()
                    val rightX = wrap((x + i).toDouble(), 0.0, (width - 1).toDouble()).toInt()

                    val hits = mutableListOf<PixelCoordinate>()

                    // Check the top and bottom rows
                    for (j in leftX..rightX) {
                        if (bitmap.getPixel(j, topY).red != 0) {
                            hits.add(PixelCoordinate(j.toFloat(), topY.toFloat()))
                        }
                        if (bitmap.getPixel(j, bottomY).red != 0) {
                            hits.add(PixelCoordinate(j.toFloat(), bottomY.toFloat()))
                        }
                    }

                    // Check the left and right columns
                    for (j in topY..bottomY) {
                        if (bitmap.getPixel(leftX, j).red != 0) {
                            hits.add(PixelCoordinate(leftX.toFloat(), j.toFloat()))
                        }
                        if (bitmap.getPixel(rightX, j).red != 0) {
                            hits.add(PixelCoordinate(rightX.toFloat(), j.toFloat()))
                        }
                    }

                    if (hits.isNotEmpty()) {
                        return hits.minByOrNull { it.distanceTo(actualPixel) } ?: actualPixel
                    }
                }
            } finally {
                bitmap?.recycle()
            }
        }

        return actualPixel
    }

    private suspend fun load(
        context: Context,
        pixel: PixelCoordinate
    ): List<TidalHarmonic> = onIO {
        val loaded = mutableListOf<TidalHarmonic>()
        val harmonics = listOf(
            TideConstituent._2N2,
            TideConstituent.J1,
            TideConstituent.K1,
            TideConstituent.K2,
            TideConstituent.M2,
            TideConstituent.M4,
            TideConstituent.MF,
            TideConstituent.MM,
            TideConstituent.N2,
            TideConstituent.O1,
            TideConstituent.P1,
            TideConstituent.Q1,
            TideConstituent.S1,
            TideConstituent.S2,
            TideConstituent.SA,
            TideConstituent.SSA,
            TideConstituent.T2
        )

        val harmonicNameMap = mutableMapOf(
            TideConstituent._2N2 to "2N2",
            TideConstituent.J1 to "J1",
            TideConstituent.K1 to "K1",
            TideConstituent.K2 to "K2",
            TideConstituent.M2 to "M2",
            TideConstituent.M4 to "M4",
            TideConstituent.MF to "MF",
            TideConstituent.MM to "MM",
            TideConstituent.N2 to "N2",
            TideConstituent.O1 to "O1",
            TideConstituent.P1 to "P1",
            TideConstituent.Q1 to "Q1",
            TideConstituent.S1 to "S1",
            TideConstituent.S2 to "S2",
            TideConstituent.SA to "SA",
            TideConstituent.SSA to "SSA",
            TideConstituent.T2 to "T2"
        )

        for (harmonic in harmonics) {
            val file = "tides/constituents-${harmonicNameMap[harmonic]}.webp"
            val data = source.read(context, file, pixel)
            val complex = ComplexNumber(data[0], data[1])
            if (data[0] == data[1]) {
                continue
            }

            loaded.add(
                TidalHarmonic(
                    harmonic,
                    complex.magnitude / 100,
                    wrap(complex.phase.toDegrees(), 0f, 360f)
                )
            )
        }

        loaded
    }

}