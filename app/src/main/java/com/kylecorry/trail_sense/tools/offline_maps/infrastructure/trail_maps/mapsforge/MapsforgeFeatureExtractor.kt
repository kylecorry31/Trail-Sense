package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps.mapsforge

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.datastore.Way
import kotlin.math.cos
import kotlin.math.hypot

private typealias Polyline = List<LatLong>

/**
 * PROOF OF CONCEPT: extracts a trail or a named POI near a location from a mapsforge map.
 *
 * Mapsforge has no OSM way IDs, splits a trail into many ways, and clips ways at tile borders with
 * an overlap of about 20 m. A trail is rebuilt by collecting every way with the same group key
 * that touches the trail, then stitching those pieces into one line.
 */
class MapsforgeFeatureExtractor(
    private val map: MapDataStore,
    /** Group by OpenAndroMaps route ref (ref_hike) before name, so a route keeps going when its way names change */
    private val groupByRoute: Boolean = true,
    private val maxTiles: Int = 400
) {

    data class ExtractedPath(
        val name: String?,
        val points: Polyline,
        val tilesRead: Int,
        val pieces: Int,
        val unusedPieces: Int,
        val maxJoinGap: Double,
        val capped: Boolean
    )

    data class ExtractedPoi(val name: String, val position: LatLong, val elevation: Float?)

    private data class StitchResult(val line: Polyline, val unusedPieces: Int, val maxJoinGap: Double)

    private val tileCache = mutableMapOf<Tile, List<Way>>()

    fun extractPath(location: LatLong, maxDistance: Double = 50.0): ExtractedPath? {
        val seed = getHighwaysNear(location)
            .minByOrNull { distanceToLine(location, points(it)) } ?: return null
        if (distanceToLine(location, points(seed)) > maxDistance) return null

        val loadedTiles = mutableSetOf<Tile>()
        val pieces = collectConnectedPieces(seed, loadedTiles)
        val stitched = stitch(pieces)
        return ExtractedPath(
            getName(seed),
            stitched.line,
            loadedTiles.size,
            pieces.size,
            stitched.unusedPieces,
            stitched.maxJoinGap,
            loadedTiles.size >= maxTiles
        )
    }

    fun extractPoi(location: LatLong, maxDistance: Double = 100.0): ExtractedPoi? {
        val poi = getTilesAround(tileFor(location))
            .flatMap { map.readMapData(it)?.pois ?: emptyList() }
            .filter { getTag(it.tags, "name") != null }
            .minByOrNull { distance(location, it.position) } ?: return null
        if (distance(location, poi.position) > maxDistance) return null
        return ExtractedPoi(
            getTag(poi.tags, "name")!!,
            poi.position,
            getTag(poi.tags, "ele")?.toFloatOrNull()
        )
    }

    /**
     * Follows the open ends of the trail tile by tile until no more connected pieces are found.
     */
    private fun collectConnectedPieces(seed: Way, loadedTiles: MutableSet<Tile>): List<Polyline> {
        val key = getGroupKey(seed)
        val pieces = linkedSetOf(points(seed))
        val unconnected = mutableSetOf<Polyline>()
        var newPieces = listOf(points(seed))

        while (newPieces.isNotEmpty() && loadedTiles.size < maxTiles) {
            val tilesToLoad = newPieces
                .flatMap { listOf(it.first(), it.last()) }
                .flatMap { getTilesAround(tileFor(it)) }
                .filter { it !in loadedTiles }
                .toSet()
            loadedTiles.addAll(tilesToLoad)

            tilesToLoad.flatMap { getHighways(it) }
                .filter { getGroupKey(it) == key }
                .map { points(it) }
                .filterTo(unconnected) { it !in pieces }

            // A piece may only connect through another piece found in this pass, so repeat until stable
            newPieces = mutableListOf()
            var hasChanged = true
            while (hasChanged) {
                hasChanged = false
                val iterator = unconnected.iterator()
                while (iterator.hasNext()) {
                    val candidate = iterator.next()
                    if (pieces.any { touches(it, candidate) }) {
                        pieces.add(candidate)
                        newPieces.add(candidate)
                        iterator.remove()
                        hasChanged = true
                    }
                }
            }
        }
        return pieces.toList()
    }

    /**
     * Grows a line from the first piece: extends the end, then reverses and extends the other end.
     * Branches that can't be added to the single line are counted as unused.
     */
    private fun stitch(pieces: List<Polyline>): StitchResult {
        val remaining = pieces.drop(1).toMutableList()
        var line = pieces.first().toMutableList()
        var maxJoinGap = 0.0
        repeat(2) {
            while (true) {
                val extension = findLongestExtension(line, remaining) ?: break
                remaining.remove(extension.piece)
                maxJoinGap = maxOf(maxJoinGap, extension.gap)
                line.addAll(extension.tail.dropWhile { distanceToLine(it, line) < DUPLICATE_POINT_DISTANCE })
            }
            line = line.asReversed().toMutableList()
        }
        return StitchResult(line, remaining.size, maxJoinGap)
    }

    private data class Extension(val piece: Polyline, val tail: Polyline, val gap: Double)

    /**
     * Finds the piece that extends the end of the line the furthest. Because pieces overlap at
     * tile borders, the end of the line can be in the middle of a piece, so only the part of the
     * piece past the end of the line (the tail) is used. Pieces fully on the line are removed.
     */
    private fun findLongestExtension(line: Polyline, remaining: MutableList<Polyline>): Extension? {
        val end = line.last()
        var best: Extension? = null
        val covered = mutableListOf<Polyline>()
        for (piece in remaining) {
            val gap = distanceToLine(end, piece)
            if (gap > JOIN_TOLERANCE) continue
            val i = getNearestSegmentIndex(end, piece)
            val tail = listOf(piece.subList(i + 1, piece.size), piece.subList(0, i + 1).reversed())
                .filter { it.isNotEmpty() && distanceToLine(it.last(), line) > JOIN_TOLERANCE }
                .maxByOrNull { it.size }
            if (tail == null) {
                if (isOnLine(piece.first(), line) && isOnLine(piece.last(), line)) {
                    covered.add(piece)
                }
                continue
            }
            if (best == null || tail.size > best.tail.size) {
                best = Extension(piece, tail, gap)
            }
        }
        remaining.removeAll(covered)
        return best
    }

    private fun getHighwaysNear(location: LatLong): List<Way> {
        return getTilesAround(tileFor(location)).flatMap { getHighways(it) }
    }

    private fun getHighways(tile: Tile): List<Way> = tileCache.getOrPut(tile) {
        if (!map.supportsTile(tile)) return@getOrPut emptyList()
        (map.readMapData(tile)?.ways ?: emptyList())
            .filter { getTag(it.tags, "highway") != null && !isClosed(it) }
    }

    private fun getGroupKey(way: Way): String {
        val keys = if (groupByRoute) {
            listOf("ref_hike", "ref", "name")
        } else {
            listOf("name", "ref_hike", "ref")
        }
        for (key in keys) {
            getTag(way.tags, key)?.let { return "$key=$it" }
        }
        // Unnamed ways only join with ways that have exactly the same tags
        return way.tags.sortedBy { it.key }.joinToString { it.key + "=" + it.value }
    }

    private fun getName(way: Way): String? {
        return getTag(way.tags, "name") ?: getTag(way.tags, "ref_hike") ?: getTag(way.tags, "ref")
    }

    private fun getTag(tags: List<Tag>, key: String): String? {
        return tags.firstOrNull { it.key == key }?.value
    }

    private fun touches(a: Polyline, b: Polyline): Boolean {
        val ends = listOf(a.first(), a.last(), b.first(), b.last())
        return ends.any { isOnLine(it, a) && isOnLine(it, b) }
    }

    private fun isOnLine(point: LatLong, line: Polyline): Boolean {
        return distanceToLine(point, line) < JOIN_TOLERANCE
    }

    private fun isClosed(way: Way): Boolean {
        val points = points(way)
        return points.size > 2 && points.first() == points.last()
    }

    private fun points(way: Way): Polyline = way.latLongs.flatMap { it.toList() }

    private fun tileFor(location: LatLong): Tile = Tile(
        MercatorProjection.longitudeToTileX(location.longitude, ZOOM),
        MercatorProjection.latitudeToTileY(location.latitude, ZOOM),
        ZOOM,
        TILE_SIZE
    )

    private fun getTilesAround(tile: Tile): List<Tile> = (-1..1).flatMap { dx ->
        (-1..1).map { dy -> Tile(tile.tileX + dx, tile.tileY + dy, ZOOM, TILE_SIZE) }
    }

    private fun getNearestSegmentIndex(point: LatLong, line: Polyline): Int {
        if (line.size == 1) return 0
        return (0..<line.lastIndex).minBy { distanceToLine(point, listOf(line[it], line[it + 1])) }
    }

    // Flat-earth approximations: accurate enough at the few-meter scale used for joining
    private fun distance(a: LatLong, b: LatLong): Double {
        val x = (b.longitude - a.longitude) * cos(Math.toRadians(a.latitude)) * METERS_PER_DEGREE_LONGITUDE
        val y = (b.latitude - a.latitude) * METERS_PER_DEGREE_LATITUDE
        return hypot(x, y)
    }

    private fun distanceToLine(point: LatLong, line: Polyline): Double {
        if (line.size == 1) return distance(point, line[0])
        val metersPerLongitude = cos(Math.toRadians(point.latitude)) * METERS_PER_DEGREE_LONGITUDE
        return line.zipWithNext().minOf { (a, b) ->
            // Project onto the segment with the point at the origin
            val ax = (a.longitude - point.longitude) * metersPerLongitude
            val ay = (a.latitude - point.latitude) * METERS_PER_DEGREE_LATITUDE
            val bx = (b.longitude - point.longitude) * metersPerLongitude
            val by = (b.latitude - point.latitude) * METERS_PER_DEGREE_LATITUDE
            val dx = bx - ax
            val dy = by - ay
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared == 0.0) 0.0 else (-(ax * dx + ay * dy) / lengthSquared).coerceIn(0.0, 1.0)
            hypot(ax + t * dx, ay + t * dy)
        }
    }

    companion object {
        // The highest base zoom in the tested map, so ways are not simplified
        private const val ZOOM: Byte = 14
        private const val TILE_SIZE = 256
        private const val JOIN_TOLERANCE = 3.0
        private const val DUPLICATE_POINT_DISTANCE = 0.5
        private const val METERS_PER_DEGREE_LATITUDE = 110540.0
        private const val METERS_PER_DEGREE_LONGITUDE = 111320.0

        /**
         * Runs the block against all visible trail maps, or returns null if there are none.
         */
        suspend fun <T> withVisibleMaps(block: (MapsforgeFeatureExtractor) -> T): T? = onIO {
            val maps = getAppService<OfflineMapService>().getRenderableTrailMaps(null)
            val files = maps.mapNotNull { MapsforgeAdapter.open(it.mapFile.path) }
            if (files.isEmpty()) return@onIO null
            val store = MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)
            files.forEachIndexed { i, file -> store.addMapDataStore(file, i == 0, i == 0) }
            try {
                block(MapsforgeFeatureExtractor(store))
            } finally {
                store.close()
            }
        }
    }
}
