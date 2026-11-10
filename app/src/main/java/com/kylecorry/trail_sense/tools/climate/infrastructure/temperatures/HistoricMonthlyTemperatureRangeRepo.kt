package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures

import android.util.Size
import com.kylecorry.luna.concurrency.Parallel
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader
import java.time.Month
import kotlin.math.max
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal object HistoricMonthlyTemperatureRangeRepo {

    // Cache
    private val cache = MemoryLRUCache<PixelCoordinate, Map<Month, Range<Temperature>>>(size = 5)

    // Image data source
    private val size = Size(576, 361)
    private const val verticalResolutionDegrees = 0.5

    // Each quantity is stored as the mean followed by the cosine and sine of each annual harmonic. The
    // mean has a polynomial of sin(latitude) subtracted from it.
    private const val harmonics = 4

    private class Model(
        val name: String,
        val a: DoubleArray,
        val b: DoubleArray,
        val latitudeTrend: DoubleArray
    ) {
        val sources = a.indices.map { i ->
            GeographicImageSource(
                EncodedDataImageReader(
                    SingleImageReader(size, AssetInputStreamable("temperatures/$name-$i.webp")),
                    decoder = EncodedDataImageReader.scaledDecoder(a[i], b[i]),
                    maxChannels = 1
                ),
                verticalResolutionDegrees = verticalResolutionDegrees
            )
        }
    }

    private val low = Model(
        "T2MMIN",
        doubleArrayOf(7.501739978937039, 5.413969975824311, 21.138197621731482, 29.566794657662687, 35.62256274775451, 88.88629055054261, 99.45095993807621, 125.79888140221307, 114.07951390332093),
        doubleArrayOf(18.745699481670854, 29.572524010505866, 7.553063666579181, 2.918301145235703, 4.930826481866384, 1.2140325705210344, 1.3450998465220136, 0.7235540946324646, 1.282492296914194),
        doubleArrayOf(26.221280651958807, -1.0207776340699977, -21.56547273042308, 4.195560445789451, -25.336009333733315)
    )

    private val high = Model(
        "T2MMAX",
        doubleArrayOf(7.709349528027072, 5.189169209136491, 20.03953671972627, 28.008160806051006, 33.61514510543693, 88.22570886867366, 101.28170061607227, 162.36093326747266, 101.82051748444593),
        doubleArrayOf(14.93543791613947, 31.90982674529993, 6.240338423615686, 3.459561824798583, 4.65315517067565, 1.4013398885726946, 1.2748637199401842, 0.7270233829816135, 1.5395452215317444),
        doubleArrayOf(29.28949352280773, 3.2050577284753032, -14.849103847257076, 0.31804828419864367, -31.381918255889566)
    )

    suspend fun getMonthlyTemperatureRanges(
        location: Coordinate
    ): Map<Month, Range<Temperature>> = onIO {
        val pixel = low.sources.first().getPixel(location)

        cache.getOrPut(pixel) {
            val lows = load(location, low)
            val highs = load(location, high)
            Month.entries.associateWith {
                val lowValue = lows[it.ordinal]
                val highValue = highs[it.ordinal]
                Range(
                    Temperature.celsius(min(lowValue, highValue)),
                    Temperature.celsius(max(lowValue, highValue))
                )
            }
        }
    }

    // The sea level temperature (°C) of each month
    private suspend fun load(location: Coordinate, model: Model): FloatArray = onIO {
        val coefficients = Parallel.map(model.sources) { it.read(location)[0].toDouble() }.toDoubleArray()

        val sinLatitude = sin(Math.toRadians(location.latitude))
        var trend = 0.0
        for (i in model.latitudeTrend.indices.reversed()) {
            trend = trend * sinLatitude + model.latitudeTrend[i]
        }
        coefficients[0] += trend

        FloatArray(12) { month ->
            val angle = 2 * PI * month / 12
            var value = coefficients[0]
            for (h in 1..harmonics) {
                value += coefficients[2 * h - 1] * cos(h * angle) + coefficients[2 * h] * sin(h * angle)
            }
            value.toFloat()
        }
    }

}
