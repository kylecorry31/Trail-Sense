package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.SolMath.wrap
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import java.io.InputStream
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

    // TODO: Extract to andromeda (nearest pixel meeting a criteria within a region - maybe just update the ImageSource with a nearest non-zero pixel option)
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
                val regionSize = 10
                bitmap = loadRegion(stream, actualPixel, regionSize, size)

                // Get the nearest non-zero pixel, wrapping around the image
                val x = regionSize
                val y = regionSize

                // Search in a grid pattern
                for (i in 1 until regionSize) {
                    val topY = y - i
                    val bottomY = y + i
                    val leftX = (x - i)
                    val rightX = (x + i)

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
                        val globalHits = hits.map {
                            // Only x is wrapped
                            val globalX =
                                wrap(
                                    actualPixel.x + it.x - regionSize,
                                    0f,
                                    size.width.toFloat()
                                )
                            val globalY = actualPixel.y + it.y - regionSize

                            PixelCoordinate(globalX, globalY)
                        }
                        return globalHits.minByOrNull { it.distanceTo(actualPixel) } ?: actualPixel
                    }
                }
            } finally {
                bitmap?.recycle()
            }
        }

        return actualPixel
    }

    // TODO: Extract to andromeda
    private fun loadRegion(
        stream: InputStream,
        center: PixelCoordinate,
        size: Int,
        fullImageSize: Size
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(2 * size + 1, 2 * size + 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = center.x.roundToInt()
        val cy = center.y.roundToInt()

        // Step 1: Calculate the region bounds
        val left = cx - size
        val top = cy - size
        val right = cx + size
        val bottom = cy + size

        // Step 2: Load as much of the region as possible
        val rect = Rect(
            max(0, left),
            max(0, top),
            min(fullImageSize.width - 1, right),
            min(fullImageSize.height - 1, bottom)
        )
        var region: Bitmap? = null
        try {
            region = BitmapUtils.decodeRegion(stream, rect, BitmapFactory.Options().also {
                it.inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: return bitmap

            val startX = if (left < 0) {
                size - cx
            } else {
                0
            }
            val startY = if (top < 0) {
                size - cy
            } else {
                0
            }

            canvas.drawBitmap(region, startX.toFloat(), startY.toFloat(), null)
        } finally {
            region?.recycle()
        }

        // Step 3: If the region extends beyond the image left/right, load the missing part from the other side
        var additionalRect: Rect? = null
        if (left < 0) {
            val remaining = size - cx
            additionalRect = Rect(
                fullImageSize.width - remaining,
                rect.top,
                fullImageSize.width - 1,
                rect.bottom
            )
        } else if (right >= fullImageSize.width) {
            val remaining = size - fullImageSize.width - cx
            additionalRect = Rect(
                0,
                rect.top,
                remaining,
                rect.bottom
            )
        }

        if (additionalRect != null) {
            try {
                region = BitmapFactory.decodeStream(
                    stream,
                    additionalRect,
                    BitmapFactory.Options().also {
                        it.inPreferredConfig = Bitmap.Config.ARGB_8888
                    }) ?: return bitmap

                val startX = if (left < 0) {
                    0
                } else {
                    rect.width()
                }

                val startY = if (top < 0) {
                    size - cy
                } else {
                    0
                }

                canvas.drawBitmap(region, startX.toFloat(), startY.toFloat(), null)
            } finally {
                region?.recycle()
            }
        }

        return bitmap
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