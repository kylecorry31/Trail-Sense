package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Size
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.square
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
    private val minAmplitude = 0f
    private val minPhase = -180f
    private val maxPhase = 180f

    private val source = GeographicImageSource(
        size,
        latitudePixelsPerDegree,
        longitudePixelsPerDegree,
        interpolate = false,
        decoder = GeographicImageSource.scaledDecoder(255.0, 0.0, false)
    )

    private val amplitudes = mutableMapOf(
        TideConstituent._2N2 to 35.5627555847168,
        TideConstituent.J1 to 20.58492088317871,
        TideConstituent.K1 to 293.3113098144531,
        TideConstituent.K2 to 136.46533203125,
        TideConstituent.M2 to 496.5640869140625,
        TideConstituent.M4 to 136.4740753173828,
        TideConstituent.MF to 26.572229385375977,
        TideConstituent.MM to 38.845985412597656,
        TideConstituent.N2 to 109.4218978881836,
        TideConstituent.O1 to 163.941162109375,
        TideConstituent.P1 to 1101.8448486328125,
        TideConstituent.Q1 to 31.578903198242188,
        TideConstituent.S1 to 4575.712890625,
        TideConstituent.S2 to 3101.253662109375,
        TideConstituent.SA to 1361.9013671875,
        TideConstituent.SSA to 8.6279935836792,
        TideConstituent.T2 to 2687.74365234375,
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
        if (source.read(context, file, location)[0] > 0f) {
            return actualPixel
        }

        val fileSystem = AssetFileSystem(context)
        fileSystem.stream(file).use { stream ->
            var bitmap: Bitmap? = null
            try {
                bitmap = BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().also {
                    it.inPreferredConfig = Bitmap.Config.ARGB_8888
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
                        if (hasValue(bitmap.getPixel(j, topY))) {
                            hits.add(PixelCoordinate(j.toFloat(), topY.toFloat()))
                        }
                        if (hasValue(bitmap.getPixel(j, bottomY))) {
                            hits.add(PixelCoordinate(j.toFloat(), bottomY.toFloat()))
                        }
                    }

                    // Check the left and right columns
                    for (j in topY..bottomY) {
                        if (hasValue(bitmap.getPixel(leftX, j))) {
                            hits.add(PixelCoordinate(leftX.toFloat(), j.toFloat()))
                        }
                        if (hasValue(bitmap.getPixel(rightX, j))) {
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

    private fun hasValue(pixel: Int): Boolean {
        return pixel.red > 0 || pixel.green > 0
    }

    private suspend fun load(
        context: Context,
        pixel: PixelCoordinate
    ): List<TidalHarmonic> = onIO {
        val loaded = mutableListOf<TidalHarmonic>()
        val harmonics = amplitudes.keys

        for (harmonic in harmonics) {
            val name = if (harmonic == TideConstituent._2N2) {
                "2N2"
            } else {
                harmonic.name
            }
            val file = "tides/constituents-${name}.webp"
            val data = source.read(context, file, pixel)
            if (data[0] <= 0) {
                continue
            }

            loaded.add(
                TidalHarmonic(
                    harmonic,
                    SolMath.lerp(
                        power(data[0].toDouble(), 4),
                        minAmplitude.toDouble(),
                        amplitudes[harmonic]!!
                    ).toFloat() / 100f,
                    wrap(SolMath.lerp(data[1], minPhase, maxPhase), 0f, 360f)
                )
            )
        }
        Log.d("TideModel", loaded.toString())
        loaded
    }

}