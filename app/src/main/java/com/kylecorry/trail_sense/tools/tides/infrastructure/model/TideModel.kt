package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.ImagePixelReader
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.sol.math.SolMath
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
    private val locationToPixelCache = LRUCache<Coordinate, PixelCoordinate?>(size = 20)

    // Image data source
    private val size = Size(720, 360)
    private val condensedSize = Size(250, 55)
    private val minAmplitude = 0f
    private val minPhase = -180.0
    private val maxPhase = 180.0
    private val searchSize = 5

    private val source = GeographicImageSource(
        size,
        precision = 0,
        interpolate = false,
        decoder = GeographicImageSource.scaledDecoder(1.0, 0.0, false)
    )

    private val imageReader = ImagePixelReader(condensedSize, interpolate = false)

    private val amplitudes = mapOf(
        TideConstituent._2N2 to 13.116927146911621,
        TideConstituent.J1 to 13.746432304382324,
        TideConstituent.K1 to 263.0484313964844,
        TideConstituent.K2 to 55.006473541259766,
        TideConstituent.M2 to 496.5640869140625,
        TideConstituent.M4 to 42.92106628417969,
        TideConstituent.MF to 17.715930938720703,
        TideConstituent.MM to 18.341230392456055,
        TideConstituent.N2 to 101.52593994140625,
        TideConstituent.O1 to 149.45440673828125,
        TideConstituent.P1 to 175.63946533203125,
        TideConstituent.Q1 to 22.561424255371094,
        TideConstituent.S1 to 381.7482604980469,
        TideConstituent.S2 to 299.8392333984375,
        TideConstituent.SA to 253.0474395751953,
        TideConstituent.SSA to 3.453131914138794,
        TideConstituent.T2 to 79.61186981201172,
    )

    private val largeAmplitudes = listOf(
        listOf(TideConstituent.S1, 181, 257, 3582),
        listOf(TideConstituent.S2, 181, 257, 3022),
        listOf(TideConstituent.T2, 181, 257, 504),
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

        if (pixel == null) {
            return@onIO emptyList()
        }

        cache.getOrPut(pixel) {
            load(context, pixel)
        }
    }

    // TODO: Extract to andromeda (nearest pixel meeting a criteria within a region - maybe just update the ImageSource with a nearest non-zero pixel option)
    private suspend fun getNearestPixel(context: Context, location: Coordinate): PixelCoordinate? {
        val actualPixel = source.getPixel(location)
        val file = "tides/tide-indices-1-2.webp"
        val sourceValue = source.read(context, file, actualPixel)
        if (sourceValue[0] > 0f || sourceValue[1] > 0f) {
            return actualPixel
        }

        val fileSystem = AssetFileSystem(context)
        fileSystem.stream(file).use { stream ->
            var bitmap: Bitmap? = null
            try {
                bitmap = loadRegion(stream, actualPixel, searchSize, size)

                // Get the nearest non-zero pixel, wrapping around the image
                val x = searchSize
                val y = searchSize

                // Search in a grid pattern
                for (i in 1 until searchSize) {
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
                                    actualPixel.x + it.x - searchSize,
                                    0f,
                                    size.width.toFloat()
                                )
                            val globalY = actualPixel.y + it.y - searchSize

                            PixelCoordinate(globalX, globalY)
                        }
                        return globalHits.minByOrNull { it.distanceTo(actualPixel) } ?: actualPixel
                    }
                }
            } finally {
                bitmap?.recycle()
            }
        }

        return null
    }

    // TODO: Extract to andromeda
    private fun loadRegion(
        stream: InputStream,
        center: PixelCoordinate,
        size: Int,
        fullImageSize: Size
    ): Bitmap {
        val cx = center.x.roundToInt()
        val cy = center.y.roundToInt()

        // Step 1: Calculate the region bounds
        val left = cx - size
        val top = cy - size
        val right = cx + size + 1
        val bottom = cy + size + 1


        // Step 2: Load as much of the region as possible
        val rect = Rect(left, top, right, bottom)

        return decodeBitmapRegionWrapped(stream, rect, fullImageSize)
    }

    fun decodeBitmapRegionWrapped(stream: InputStream, rect: Rect, imageSize: Size): Bitmap {
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val width = rect.width()
        val height = rect.height()
        val fullImageWidth = imageSize.width

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val rectsToLoad = mutableListOf<Pair<Point, Rect>>()

        // Center
        val centerIntersection = getIntersection(rect, imageSize)
        val centerOffsetX = centerIntersection.left - left
        val centerOffsetY = centerIntersection.top - top
        if (centerIntersection.width() > 0 && centerIntersection.height() > 0) {
            rectsToLoad.add(
                Pair(
                    Point(centerOffsetX, centerOffsetY),
                    centerIntersection
                )
            )
        }

        // Left (display the right side of the image)
        if (centerOffsetX > 0) {
            val leftRect = Rect(
                fullImageWidth - centerOffsetX,
                centerIntersection.top,
                fullImageWidth,
                centerIntersection.bottom
            )
            rectsToLoad.add(
                Pair(
                    Point(0, centerOffsetY),
                    leftRect
                )
            )
        }

        // Right (display the left side of the image)
        if (right > fullImageWidth) {
            val rightRect = Rect(
                0,
                centerIntersection.top,
                right - fullImageWidth,
                centerIntersection.bottom
            )
            rectsToLoad.add(
                Pair(
                    Point(centerIntersection.width() + centerOffsetX, centerOffsetY),
                    rightRect
                )
            )
        }

        for ((offset, rectToLoad) in rectsToLoad) {
            val bitmap = BitmapUtils.decodeRegion(
                stream,
                rectToLoad,
                null,
                autoClose = false,
                enforceBounds = true
            ) ?: continue
            canvas.drawBitmap(bitmap, offset.x.toFloat(), offset.y.toFloat(), null)
            bitmap.recycle()
        }

        return resultBitmap
    }

    private fun getIntersection(rect: Rect, imageSize: Size): Rect {
        val left = max(0, rect.left)
        val top = max(0, rect.top)
        val right = min(imageSize.width, rect.right)
        val bottom = min(imageSize.height, rect.bottom)
        return Rect(left, top, right, bottom)
    }

    private fun hasValue(pixel: Int): Boolean {
        return pixel.red > 0 || pixel.green > 0
    }

    private suspend fun load(
        context: Context,
        pixel: PixelCoordinate
    ): List<TidalHarmonic> = onIO {

        // Step 1: Get the indices into the amplitudes/phase array
        val indicesFile = "tides/tide-indices-1-2.webp"
        val indices = source.read(context, indicesFile, pixel)
        val x = indices[0].toInt() - 1
        val y = indices[1].toInt() - 1

        // For each constituent, load the amplitude and phase (grouped 4 per file)
        val constituents = listOf(
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

        val loaded = mutableListOf<TidalHarmonic>()
        var i = 1
        val constituentsPerImage = 3
        val fileSystem = AssetFileSystem(context)
        for (groupedConstituents in constituents.chunked(constituentsPerImage)) {
            val amplitudeFile = "tides/tide-amplitudes-${i}-${i + constituentsPerImage - 1}.webp"
            val phaseFile = "tides/tide-phases-${i}-${i + constituentsPerImage - 1}.webp"
            i += constituentsPerImage
            val amplitudePixel = fileSystem.stream(amplitudeFile)
                .use { imageReader.getPixel(it, x.toFloat(), y.toFloat(), false) } ?: continue
            val phasePixel = fileSystem.stream(phaseFile)
                .use { imageReader.getPixel(it, x.toFloat(), y.toFloat(), false) } ?: continue

            // Construct the harmonics
            for (j in groupedConstituents.indices) {
                val harmonic = groupedConstituents[j]

                // If there's a match in the large amplitudes array, use that for the amplitude
                val largeAmplitude =
                    (largeAmplitudes.firstOrNull { it[0] == harmonic && it[1] == pixel.x.roundToInt() && it[2] == pixel.y.roundToInt() }
                        ?.get(3) as Double?)?.toFloat()

                val amplitude = (largeAmplitude ?: SolMath.lerp(
                    (getColorIndex(amplitudePixel, j).toDouble() / 255),
                    minAmplitude.toDouble(),
                    amplitudes[harmonic]!!
                ).toFloat()) / 100f
                val phase = wrap(
                    SolMath.lerp(
                        (getColorIndex(phasePixel, j).toDouble() / 255),
                        minPhase,
                        maxPhase
                    ), 0.0, 360.0
                ).toFloat()

                loaded.add(TidalHarmonic(harmonic, amplitude, phase))
            }
        }

        Log.d("TideModel", loaded.toString())
        loaded
    }

    private fun getColorIndex(color: Int, index: Int): Int {
        return when (index) {
            0 -> color.red
            1 -> color.green
            2 -> color.blue
            3 -> color.alpha
            else -> 0
        }
    }

}