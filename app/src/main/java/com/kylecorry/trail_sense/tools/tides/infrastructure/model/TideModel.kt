package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import android.content.Context
import android.util.Size
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.kylecorry.andromeda.bitmaps.ImagePixelReader
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.GeographicImageUtils
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import kotlin.math.roundToInt

object TideModel {

    // Cache
    private val cache = MemoryLRUCache<PixelCoordinate, TideModelResult>(size = 5)
    private val locationToPixelCache = MemoryLRUCache<Coordinate, PixelCoordinate?>(size = 20)

    // Image data source
    private val size = Size(1440, 720)
    private val condensedSize = Size(128, 172)
    private const val MIN_AMPLITUDE = 0f
    private val minPhase = -180.0
    private const val MAX_PHASE = 180.0
    private const val SEARCH_SIZE = 8

    private val source = GeographicImageSource(
        EncodedDataImageReader(
            SingleImageReader(size, AssetInputStreamable("tides/tide-indices-1-2.webp")),
            decoder = EncodedDataImageReader.scaledDecoder(1.0, 0.0, false),
            maxChannels = 2
        ),
        precision = 0,
        valuePixelOffset = 0.25f,
        interpolationOrder = 0
    )

    private val imageReader = ImagePixelReader(condensedSize, interpolate = false)

    private val amplitudes = mapOf(
        TideConstituent.TWO_N2 to 35.562755148866074,
        TideConstituent.J1 to 19.412978757674825,
        TideConstituent.K1 to 293.3113265854845,
        TideConstituent.K2 to 136.46533334158224,
        TideConstituent.M2 to 474.8659738027007,
        TideConstituent.M4 to 136.47406850713315,
        TideConstituent.MF to 26.57222851875556,
        TideConstituent.MM to 38.845984640449046,
        TideConstituent.N2 to 107.99308333748937,
        TideConstituent.O1 to 163.9411506321465,
        TideConstituent.P1 to 282.97924579874797,
        TideConstituent.Q1 to 31.17831745303195,
        TideConstituent.S1 to 368.84208613374,
        TideConstituent.S2 to 321.04079565433807,
        TideConstituent.SA to 360.6890664468158,
        TideConstituent.SSA to 8.62799275024558,
        TideConstituent.T2 to 487.1148963104638,
    )

    private data class LargeAmplitude(
        val constituent: TideConstituent,
        val x: Int,
        val y: Int,
        val amplitude: Double
    )

    private val largeAmplitudes = listOf(
        LargeAmplitude(TideConstituent.P1, 105, 75, 774.0),
        LargeAmplitude(TideConstituent.P1, 6, 76, 909.0),
        LargeAmplitude(TideConstituent.P1, 7, 76, 688.0),
        LargeAmplitude(TideConstituent.P1, 29, 76, 1101.0),
        LargeAmplitude(TideConstituent.P1, 30, 76, 891.0),
        LargeAmplitude(TideConstituent.S1, 105, 75, 619.0),
        LargeAmplitude(TideConstituent.S1, 6, 76, 1612.0),
        LargeAmplitude(TideConstituent.S1, 7, 76, 1150.0),
        LargeAmplitude(TideConstituent.S1, 8, 76, 867.0),
        LargeAmplitude(TideConstituent.S1, 29, 76, 1598.0),
        LargeAmplitude(TideConstituent.S1, 30, 76, 1828.0),
        LargeAmplitude(TideConstituent.S1, 40, 126, 1329.0),
        LargeAmplitude(TideConstituent.S1, 41, 126, 1946.0),
        LargeAmplitude(TideConstituent.S1, 42, 126, 508.0),
        LargeAmplitude(TideConstituent.S1, 69, 126, 3480.0),
        LargeAmplitude(TideConstituent.S1, 70, 126, 3233.0),
        LargeAmplitude(TideConstituent.S1, 14, 127, 883.0),
        LargeAmplitude(TideConstituent.S1, 15, 127, 2436.0),
        LargeAmplitude(TideConstituent.S1, 16, 127, 4575.0),
        LargeAmplitude(TideConstituent.S2, 105, 75, 1650.0),
        LargeAmplitude(TideConstituent.S2, 6, 76, 2851.0),
        LargeAmplitude(TideConstituent.S2, 7, 76, 2097.0),
        LargeAmplitude(TideConstituent.S2, 8, 76, 1238.0),
        LargeAmplitude(TideConstituent.S2, 29, 76, 3101.0),
        LargeAmplitude(TideConstituent.S2, 30, 76, 3030.0),
        LargeAmplitude(TideConstituent.S2, 40, 126, 2626.0),
        LargeAmplitude(TideConstituent.S2, 41, 126, 2112.0),
        LargeAmplitude(TideConstituent.S2, 42, 126, 516.0),
        LargeAmplitude(TideConstituent.S2, 69, 126, 2911.0),
        LargeAmplitude(TideConstituent.S2, 70, 126, 2636.0),
        LargeAmplitude(TideConstituent.S2, 14, 127, 559.0),
        LargeAmplitude(TideConstituent.S2, 15, 127, 1681.0),
        LargeAmplitude(TideConstituent.S2, 16, 127, 2509.0),
        LargeAmplitude(TideConstituent.S2, 17, 127, 1184.0),
        LargeAmplitude(TideConstituent.SA, 105, 75, 996.0),
        LargeAmplitude(TideConstituent.SA, 6, 76, 1106.0),
        LargeAmplitude(TideConstituent.SA, 7, 76, 843.0),
        LargeAmplitude(TideConstituent.SA, 29, 76, 1361.0),
        LargeAmplitude(TideConstituent.SA, 30, 76, 1069.0),
        LargeAmplitude(TideConstituent.T2, 80, 75, 710.0),
        LargeAmplitude(TideConstituent.T2, 105, 75, 1959.0),
        LargeAmplitude(TideConstituent.T2, 6, 76, 2186.0),
        LargeAmplitude(TideConstituent.T2, 7, 76, 1662.0),
        LargeAmplitude(TideConstituent.T2, 8, 76, 652.0),
        LargeAmplitude(TideConstituent.T2, 29, 76, 2687.0),
        LargeAmplitude(TideConstituent.T2, 30, 76, 2115.0),
        LargeAmplitude(TideConstituent.T2, 16, 127, 628.0),
        LargeAmplitude(TideConstituent.T2, 17, 127, 503.0),
    )

    suspend fun getHarmonics(
        context: Context,
        location: Coordinate
    ): TideModelResult? = onIO {
        val pixel = locationToPixelCache.getOrPut(
            Coordinate(
                location.latitude.roundPlaces(1),
                location.longitude.roundPlaces(1)
            )
        ) {
            GeographicImageUtils.getNearestPixelOfAsset(
                source,
                context,
                location,
                "tides/tide-indices-1-2.webp",
                SEARCH_SIZE,
                hasValue = { it.red > 0 || it.green > 0 },
                hasMappedValue = { it[0] > 0f || it[1] > 0f }
            )
        } ?: return@onIO null

        val cached = cache.get(pixel)
        if (cached != null) {
            return@onIO cached
        }

        val tideLocation = source.getLocation(pixel)
        val harmonics = load(context, pixel)
        val result = TideModelResult(tideLocation, harmonics)
        cache.put(pixel, result)
        result
    }

    private suspend fun load(
        context: Context,
        pixel: PixelCoordinate
    ): List<TidalHarmonic> = onIO {

        // Step 1: Get the indices into the amplitudes/phase array
        val indices = source.read(pixel)
        val x = indices[0].toInt() - 1
        val y = indices[1].toInt() - 1

        // For each constituent, load the amplitude and phase (grouped 4 per file)
        val constituents = listOf(
            TideConstituent.TWO_N2,
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
                    largeAmplitudes.firstOrNull {
                        it.constituent == harmonic &&
                                it.x == x &&
                                it.y == y
                    }?.amplitude?.toFloat()

                val amplitude = (largeAmplitude ?: Interpolation.lerp(
                    (getColorIndex(amplitudePixel, j).toDouble() / 255),
                    MIN_AMPLITUDE.toDouble(),
                    amplitudes[harmonic]!!
                ).toFloat()) / 100f
                val phase = wrap(
                    Interpolation.lerp(
                        (getColorIndex(phasePixel, j).toDouble() / 255),
                        minPhase,
                        MAX_PHASE
                    ), 0.0, 360.0
                ).toFloat()

                loaded.add(TidalHarmonic(harmonic, amplitude, phase))
            }
        }

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
