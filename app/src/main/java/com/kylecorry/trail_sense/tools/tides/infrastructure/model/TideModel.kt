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
    private const val latitudePixelsPerDegree = 2.0
    private const val longitudePixelsPerDegree = 2.0
    private val size = Size(720, 360)

    private val scaleMap = mutableMapOf(
        TideConstituent._2N2 to Pair(11.356071472167969, 11.140035629272461),
        TideConstituent.J1 to Pair(10.40841007232666, 13.524446487426758),
        TideConstituent.K1 to Pair(0.49884647130966187, 254.1129608154297),
        TideConstituent.K2 to Pair(2.659036874771118, 51.51428985595703),
        TideConstituent.M2 to Pair(0.2973381578922272, 393.4219665527344),
        TideConstituent.M4 to Pair(3.337411642074585, 42.507568359375),
        TideConstituent.MF to Pair(8.843782424926758, 11.178329467773438),
        TideConstituent.MM to Pair(11.158853530883789, 5.290022373199463),
        TideConstituent.N2 to Pair(1.4540624618530273, 73.89794158935547),
        TideConstituent.O1 to Pair(1.0482910871505737, 99.1529541015625),
        TideConstituent.P1 to Pair(1.1195718050003052, 89.13236236572266),
        TideConstituent.Q1 to Pair(6.170979976654053, 19.15773582458496),
        TideConstituent.S1 to Pair(0.05546007305383682, 1235.134521484375),
        TideConstituent.S2 to Pair(0.05975821986794472, 2014.5478515625),
        TideConstituent.SA to Pair(1.1215876340866089, 197.72166442871094),
        TideConstituent.SSA to Pair(38.392425537109375, 3.450453281402588),
        TideConstituent.T2 to Pair(0.4328470230102539, 93.9348373413086),
    )

    private val sourceMap = scaleMap.mapValues {
        GeographicImageSource(
            size,
            latitudePixelsPerDegree,
            longitudePixelsPerDegree,
            interpolate = false,
            decoder = GeographicImageSource.scaledDecoder(it.value.first, it.value.second, false)
        )
    }

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
        val actualPixel = sourceMap[TideConstituent.M2]!!.getPixel(location)
        val file = "tides/constituents-M2.webp"
        if (sourceMap[TideConstituent.M2]!!.read(context, file, location)[0] != 0f) {
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
        val harmonics = scaleMap.keys

        for (harmonic in harmonics) {
            val name = if (harmonic == TideConstituent._2N2) {
                "2N2"
            } else {
                harmonic.name
            }
            val file = "tides/constituents-${name}.webp"
            val data = sourceMap[harmonic]?.read(context, file, pixel) ?: continue
            val complex = ComplexNumber(data[0], data[1])
            if (data[0] == 0f) {
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
        println(loaded)
        loaded
    }

}