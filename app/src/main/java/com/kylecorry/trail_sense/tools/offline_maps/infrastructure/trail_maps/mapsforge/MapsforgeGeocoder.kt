package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.mapsforge

import android.util.Log
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.reader.MapFile
import java.io.File
import kotlin.math.min

/**
 * Geocoding on top of Mapsforge's readNamedItems. The map file format has no name index, so every
 * lookup reads and filters the named items of the tiles it covers.
 */
class MapsforgeGeocoder {

    data class Place(val name: String, val kind: String, val coordinate: Coordinate)

    data class SearchResult(val places: List<Place>, val ms: Long)

    suspend fun search(
        query: String,
        near: Coordinate,
        maxChunks: Int = Int.MAX_VALUE,
        limit: Int = 20
    ): SearchResult = onIO {
        val start = System.currentTimeMillis()
        val normalizedQuery = query.trim().lowercase()
        val closestByNameAndKind = HashMap<String, Place>()

        withMaps { map ->
            for ((upperLeft, lowerRight) in chunksByDistance(map, near).take(maxChunks)) {
                forEachNamed(map.readNamedItems(upperLeft, lowerRight)) { place ->
                    if (!place.name.lowercase().contains(normalizedQuery)) return@forEachNamed
                    val key = "${place.name}|${place.kind}"
                    val existing = closestByNameAndKind[key]
                    if (existing == null || place.distanceTo(near) < existing.distanceTo(near)) {
                        closestByNameAndKind[key] = place
                    }
                }
            }
        }

        val places = closestByNameAndKind.values
            .sortedWith(
                compareBy<Place> { matchRank(it.name.lowercase(), normalizedQuery) }
                    .thenBy { it.distanceTo(near) }
            )
            .take(limit)
        val ms = System.currentTimeMillis() - start
        Log.d(TAG, "search '$query': ${places.size} results in $ms ms")
        SearchResult(places, ms)
    }

    suspend fun reverse(location: Coordinate): String? = onIO {
        val start = System.currentTimeMillis()
        var description: String? = null

        withMaps { map ->
            if (description != null) return@withMaps
            val (tileX, tileY) = tileOf(location)
            val items = map.readNamedItems(
                Tile(tileX - REVERSE_RADIUS, tileY - REVERSE_RADIUS, ZOOM, TILE_SIZE),
                Tile(tileX + REVERSE_RADIUS, tileY + REVERSE_RADIUS, ZOOM, TILE_SIZE)
            )
            description = describe(items, location)
        }

        Log.d(TAG, "reverse $location: $description in ${System.currentTimeMillis() - start} ms")
        description
    }

    private fun describe(items: MapReadResult, location: Coordinate): String? {
        val containingArea = items.ways
            .filter { way -> hasName(way.tags) && way.tags.any { it.key in AREA_KEYS } }
            .filter { way -> way.latLongs.isNotEmpty() && contains(way.latLongs[0], location) }
            .minByOrNull { way -> boundingBoxArea(way.latLongs[0]) }
        val nearestRoad = items.ways
            .filter { way -> hasName(way.tags) && way.tags.any { it.key == "highway" } }
            .minByOrNull { way -> distanceToLine(way.latLongs[0], location) }
        val nearestPlace = items.pois
            .filter { poi -> hasName(poi.tags) && poi.tags.any { it.key == "place" } }
            .minByOrNull { poi -> location.distanceTo(poi.position.toCoordinate()) }

        val parts = mutableListOf<String>()
        containingArea?.let { parts.add(nameOf(it.tags)!!) }
        nearestRoad?.let {
            if (distanceToLine(it.latLongs[0], location) < ROAD_MAX_DISTANCE_METERS) {
                parts.add("near ${nameOf(it.tags)}")
            }
        }
        nearestPlace?.let {
            val kilometers = (location.distanceTo(it.position.toCoordinate()) / 1000).toInt()
            parts.add("$kilometers km from ${nameOf(it.tags)}")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private suspend fun withMaps(block: (MapFile) -> Unit) {
        val trailMaps = getAppService<OfflineMapService>().getRenderableTrailMaps(null)
        val mapFiles = trailMaps.mapNotNull { MapsforgeAdapter.open(it.mapFile.path) }
            .ifEmpty { listOfNotNull(File(HARDCODED_MAP).takeIf { it.exists() }?.let { MapFile(it) }) }
        try {
            mapFiles.forEach(block)
        } finally {
            mapFiles.forEach { it.close() }
        }
    }

    // Closest chunks first so that nearby matches are found before the scan reaches distant ones
    private fun chunksByDistance(map: MapFile, near: Coordinate): List<Pair<Tile, Tile>> {
        val bounds = map.boundingBox()
        val minX = MercatorProjection.longitudeToTileX(bounds.minLongitude, ZOOM)
        val maxX = MercatorProjection.longitudeToTileX(bounds.maxLongitude, ZOOM)
        val minY = MercatorProjection.latitudeToTileY(bounds.maxLatitude, ZOOM)
        val maxY = MercatorProjection.latitudeToTileY(bounds.minLatitude, ZOOM)
        val (nearX, nearY) = tileOf(near)

        val chunkOrigins = mutableListOf<Pair<Int, Int>>()
        for (y in minY..maxY step CHUNK_SIZE) {
            for (x in minX..maxX step CHUNK_SIZE) {
                chunkOrigins.add(x to y)
            }
        }

        return chunkOrigins
            .sortedBy { (x, y) ->
                val dx = x + CHUNK_SIZE / 2 - nearX
                val dy = y + CHUNK_SIZE / 2 - nearY
                dx.toDouble() * dx + dy.toDouble() * dy
            }
            .map { (x, y) ->
                Tile(x, y, ZOOM, TILE_SIZE) to
                        Tile(min(x + CHUNK_SIZE - 1, maxX), min(y + CHUNK_SIZE - 1, maxY), ZOOM, TILE_SIZE)
            }
    }

    private fun forEachNamed(items: MapReadResult, action: (Place) -> Unit) {
        for (poi in items.pois) {
            val name = nameOf(poi.tags) ?: continue
            action(Place(name, kindOf(poi.tags), poi.position.toCoordinate()))
        }
        for (way in items.ways) {
            val name = nameOf(way.tags) ?: continue
            val ring = way.latLongs.firstOrNull() ?: continue
            val center = way.labelPosition ?: boundingBoxCenter(ring) ?: continue
            action(Place(name, kindOf(way.tags), center.toCoordinate()))
        }
    }

    private fun matchRank(name: String, query: String): Int {
        return when {
            name == query -> 0
            name.startsWith(query) -> 1
            else -> 2
        }
    }

    private fun Place.distanceTo(location: Coordinate): Float = coordinate.distanceTo(location)

    private fun LatLong.toCoordinate() = Coordinate(latitude, longitude)

    private fun hasName(tags: List<Tag>): Boolean = nameOf(tags) != null

    private fun nameOf(tags: List<Tag>): String? = tags.firstOrNull { it.key == "name" }?.value

    private fun kindOf(tags: List<Tag>): String {
        tags.firstOrNull { it.key == "place" }?.let { return "place=${it.value}" }
        return tags.firstOrNull { it.key in KIND_KEYS }?.let { "${it.key}=${it.value}" } ?: "other"
    }

    private fun tileOf(location: Coordinate): Pair<Int, Int> {
        return MercatorProjection.longitudeToTileX(location.longitude, ZOOM) to
                MercatorProjection.latitudeToTileY(location.latitude, ZOOM)
    }

    private fun boundingBoxCenter(ring: Array<LatLong>): LatLong? {
        if (ring.isEmpty()) return null
        return LatLong(
            (ring.minOf { it.latitude } + ring.maxOf { it.latitude }) / 2,
            (ring.minOf { it.longitude } + ring.maxOf { it.longitude }) / 2
        )
    }

    private fun boundingBoxArea(ring: Array<LatLong>): Double {
        val height = ring.maxOf { it.latitude } - ring.minOf { it.latitude }
        val width = ring.maxOf { it.longitude } - ring.minOf { it.longitude }
        return height * width
    }

    private fun contains(ring: Array<LatLong>, point: Coordinate): Boolean {
        if (ring.size < 4 || ring.first() != ring.last()) return false
        var inside = false
        var previous = ring.last()
        for (current in ring) {
            val crossesLatitude = (current.latitude > point.latitude) != (previous.latitude > point.latitude)
            if (crossesLatitude &&
                point.longitude < (previous.longitude - current.longitude) *
                (point.latitude - current.latitude) / (previous.latitude - current.latitude) +
                current.longitude
            ) {
                inside = !inside
            }
            previous = current
        }
        return inside
    }

    // Vertices only, so long segments are underestimated
    private fun distanceToLine(line: Array<LatLong>, point: Coordinate): Float {
        return line.minOf { point.distanceTo(it.toCoordinate()) }
    }

    companion object {
        private const val TAG = "MapsforgeGeocoder"
        private const val ZOOM: Byte = 14
        private const val TILE_SIZE = 256
        private const val CHUNK_SIZE = 4
        private const val REVERSE_RADIUS = 2
        private const val ROAD_MAX_DISTANCE_METERS = 150
        private const val HARDCODED_MAP = "/sdcard/Android/data/com.kylecorry.trail_sense/files/ny.map"
        private val AREA_KEYS = setOf("leisure", "landuse", "natural", "boundary", "tourism", "amenity", "place")
        private val KIND_KEYS = setOf(
            "natural", "waterway", "highway", "leisure", "tourism", "amenity", "railway", "landuse",
            "boundary", "building", "shop"
        )
    }
}
