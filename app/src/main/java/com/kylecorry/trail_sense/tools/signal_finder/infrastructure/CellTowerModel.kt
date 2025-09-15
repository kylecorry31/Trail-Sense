package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.data.AssetInputStreamable
import com.kylecorry.trail_sense.shared.data.EncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.GeographicImageSource
import com.kylecorry.trail_sense.shared.data.SingleImageReader

object CellTowerModel {

    // Cache
    private val cache = LRUCache<PixelCoordinate, List<CellNetwork>>(size = 10)
    private val locationToPixelCache = LRUCache<Coordinate, PixelCoordinate?>(size = 20)

    // Image data source
    private val resolution = 0.03
    private val pixelsPerDegree = 1 / resolution
    private val size = Size((360 * pixelsPerDegree).toInt(), (180 * pixelsPerDegree).toInt())

    val accuracy = Distance.nauticalMiles(resolution.toFloat() * 60 / 2f).meters()

    private val source = GeographicImageSource(
        EncodedDataImageReader(
            SingleImageReader(size, AssetInputStreamable("cell_towers.webp")),
            decoder = EncodedDataImageReader.scaledDecoder(1.0, 0.0, false),
            maxChannels = 1
        ),
        interpolationOrder = 0
    )

    // TODO: Load the whole region of the image and get the towers from it
    suspend fun getTowers(
        geofence: Geofence,
        count: Int? = null
    ): List<Pair<Coordinate, List<CellNetwork>>> = onIO {
        val bounds = CoordinateBounds.from(geofence)
        val locations = mutableListOf<Coordinate>()

        val latitudes = Interpolation.getMultiplesBetween(
            bounds.south - resolution,
            bounds.north + resolution,
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween(
            bounds.west - resolution,
            bounds.east + resolution,
            resolution
        )

        latitudes.forEach { lat ->
            longitudes.forEach { lon ->
                locations.add(Coordinate(lat, lon))
            }
        }

        // Remove locations that are outside the geofence
        locations.removeIf { !geofence.contains(it) }

        getTowers(locations)
            .sortedBy { it.first.distanceTo(geofence.center) }
            .take(count ?: Int.MAX_VALUE)
    }

    suspend fun getTowers(
        location: Coordinate
    ): Pair<Coordinate, List<CellNetwork>> = onIO {
        val rounded = location.copy(
            latitude = location.latitude.roundNearest(resolution),
            longitude = location.longitude.roundNearest(resolution)
        )
        val pixel = locationToPixelCache.getOrPut(rounded) {
            source.getPixel(rounded)
        }

        if (pixel == null) {
            return@onIO rounded to emptyList()
        }

        rounded to cache.getOrPut(pixel) {
            load(pixel)
        }
    }

    private suspend fun getTowers(
        locations: List<Coordinate>
    ): List<Pair<Coordinate, List<CellNetwork>>> = onIO {
        val pixelsToLoad = mutableMapOf<PixelCoordinate, Coordinate>()

        // Map locations to pixels
        for (location in locations) {
            val rounded = location.copy(
                latitude = location.latitude.roundNearest(resolution),
                longitude = location.longitude.roundNearest(resolution)
            )
            val pixel = locationToPixelCache.getOrPut(rounded) {
                source.getPixel(rounded)
            }
            if (pixel != null) {
                pixelsToLoad[pixel] = rounded
            }
        }

        val results = source.read(pixelsToLoad.keys.toList())
            .mapNotNull {
                val data = it.second
                if (data.isEmpty()) {
                    return@mapNotNull null
                }
                val location = pixelsToLoad[it.first] ?: return@mapNotNull null
                val networks = getTowersFromPixel(data[0].toInt())
                if (networks.isEmpty()) {
                    return@mapNotNull null
                }
                location to networks
            }

        results
    }

    private suspend fun load(
        pixel: PixelCoordinate
    ): List<CellNetwork> = onIO {
        val data = source.read(pixel)
        if (data.isEmpty()) {
            return@onIO emptyList()
        }
        getTowersFromPixel(data[0].toInt())
    }

    private fun getTowersFromPixel(value: Int): List<CellNetwork> {
        val networkMap = mapOf(
            1 to CellNetwork.Gsm,
            2 to CellNetwork.Wcdma,
            4 to CellNetwork.Lte,
            8 to CellNetwork.Nr,
            16 to CellNetwork.Cdma
        )

        val networks = mutableListOf<CellNetwork>()
        for ((key, network) in networkMap) {
            if (value and key == key) {
                networks.add(network)
            }
        }
        return networks
    }

}