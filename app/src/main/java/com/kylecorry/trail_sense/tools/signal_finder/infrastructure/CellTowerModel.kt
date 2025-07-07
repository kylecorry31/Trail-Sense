package com.kylecorry.trail_sense.tools.signal_finder.infrastructure

import android.content.Context
import android.util.Size
import com.kylecorry.andromeda.core.cache.LRUCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.data.GeographicImageSource

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
        size,
        interpolate = false,
        decoder = GeographicImageSource.scaledDecoder(1.0, 0.0, false),
        latitudePixelsPerDegree = pixelsPerDegree,
        longitudePixelsPerDegree = pixelsPerDegree
    )

    // TODO: Load the whole region of the image and get the towers from it
    suspend fun getTowers(
        context: Context,
        geofence: Geofence,
        count: Int? = null
    ): List<Pair<Coordinate, List<CellNetwork>>> = onIO {
        val bounds = CoordinateBounds.from(geofence)
        val locations = mutableListOf<Coordinate>()
        var lon = bounds.west.roundNearest(resolution)
        var lat = bounds.north.roundNearest(resolution)
        while (lon <= bounds.east) {
            while (lat >= bounds.south) {
                locations.add(Coordinate(lat, lon))
                lat -= resolution
            }
            lat = bounds.north
            lon += resolution
        }

        // Remove locations that are outside the geofence
        locations.removeIf { !geofence.contains(it) }

        val towers = mutableListOf<Pair<Coordinate, List<CellNetwork>>>()

        val sortedLocations = locations
            .sortedBy { it.distanceTo(geofence.center) }
            .distinct()

        for (location in sortedLocations) {
            val tower = getTowers(context, location)
            if (tower.second.isNotEmpty()) {
                towers.add(tower)
            }
            if (count != null && towers.size >= count) {
                break
            }
        }

        towers
    }

    suspend fun getTowers(
        context: Context,
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
            load(context, pixel)
        }
    }

    private suspend fun load(
        context: Context,
        pixel: PixelCoordinate
    ): List<CellNetwork> = onIO {
        val data = source.read(context, "cell_towers.webp", pixel)
        if (data.isEmpty()) {
            return@onIO emptyList()
        }
        val value = data[0].toInt()
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
        networks
    }

}