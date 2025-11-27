package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures

import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import java.time.Month
import kotlin.math.max
import kotlin.math.min

internal object HistoricMonthlyTemperatureRangeRepo {

    // Cache
    private val cache = LRUCache<PixelCoordinate, Map<Month, Range<Temperature>>>(size = 5)

    // Image data source
    private const val highA = 1.3492063283920288
    private const val highB = 48.0
    private const val lowA = 1.4010988473892212
    private const val lowB = 56.0
    private val size = Size(576, 361)

    private val extensionMap = mapOf(
        "1-3" to Triple(Month.JANUARY, Month.FEBRUARY, Month.MARCH),
        "4-6" to Triple(Month.APRIL, Month.MAY, Month.JUNE),
        "7-9" to Triple(Month.JULY, Month.AUGUST, Month.SEPTEMBER),
        "10-12" to Triple(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER)
    )

    private const val lowType = "T2MMIN"
    private const val highType = "T2MMAX"

    private val lowSources = extensionMap.map { (extension, months) ->
        val file = "temperatures/${lowType}-${extension}.webp"
        months to GeographicImageSource(
            EncodedDataImageReader(
                SingleImageReader(size, AssetInputStreamable(file)),
                decoder = EncodedDataImageReader.scaledDecoder(lowA, lowB),
                maxChannels = 3
            )
        )
    }

    private val highSources = extensionMap.map { (extension, months) ->
        val file = "temperatures/${highType}-${extension}.webp"
        months to GeographicImageSource(
            EncodedDataImageReader(
                SingleImageReader(size, AssetInputStreamable(file)),
                decoder = EncodedDataImageReader.scaledDecoder(highA, highB),
                maxChannels = 3
            )
        )
    }

    suspend fun getMonthlyTemperatureRanges(
        location: Coordinate
    ): Map<Month, Range<Temperature>> = onIO {
        val pixel = lowSources.first().second.getPixel(location)

        cache.getOrPut(pixel) {
            val lows = load(location, lowType)
            val highs = load(location, highType)
            Month.entries.associateWith {
                val low = lows[it] ?: 0f
                val high = highs[it] ?: 0f
                Range(
                    Temperature.from(min(low, high), TemperatureUnits.F).celsius(),
                    Temperature.from(max(low, high), TemperatureUnits.F).celsius()
                )
            }
        }
    }

    private suspend fun load(
        location: Coordinate,
        type: String
    ): Map<Month, Float> = onIO {
        val loaded = mutableMapOf<Month, Float>()

        val sources = if (type == lowType) lowSources else highSources

        for ((months, source) in sources) {
            val data = source.read(location)
            loaded[months.first] = data[0]
            loaded[months.second] = data[1]
            loaded[months.third] = data[2]
        }

        loaded
    }

}