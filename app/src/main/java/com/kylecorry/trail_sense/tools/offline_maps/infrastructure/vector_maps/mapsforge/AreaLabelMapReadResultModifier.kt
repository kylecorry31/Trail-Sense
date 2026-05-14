package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.LRUCache
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.datastore.PointOfInterest
import org.mapsforge.map.datastore.Way
import org.mapsforge.map.rendertheme.RenderContext

/**
 * Adds POIs to the map read result for the center of named areas.
 */
class AreaLabelMapReadResultModifier(
    val impactedAreas: Map<String, Set<String>>,
    val fullAreaZoomLevel: Byte
) : MapReadResultModifier {

    private val boundaryLabelNodes = LRUCache<Tile, List<PointOfInterest>>(100)

    override fun process(
        renderContext: RenderContext,
        mapReadResult: MapReadResult,
        mapDataStore: MapDataStore
    ) {
        val tile = renderContext.rendererJob.tile
        if (tile.zoomLevel < fullAreaZoomLevel) {
            return
        }
        mapReadResult.pois.addAll(getPointsOfInterest(tile, mapDataStore))
    }

    private fun shouldProcess(way: Way): Boolean {
        var hasMatchingRule = false
        var hasName = false
        for (tag in way.tags) {
            if (!hasMatchingRule) {
                hasMatchingRule = impactedAreas[tag.key]?.contains(tag.value) == true
            }
            if (!hasName) {
                hasName = tag.key == "name"
            }
            if (hasMatchingRule && hasName) return true
        }
        return false
    }

    private fun createPointsOfInterestFromWays(ways: List<Way>): List<PointOfInterest> {
        val clusters = getWayClusters(ways)
        return clusters.mapNotNull { cluster ->
            val template = cluster.first()
            val center = template.labelPosition ?: WayCentroidCalculator.calculate(cluster) ?: return@mapNotNull null
            PointOfInterest(
                template.layer,
                template.tags + AREA_LABEL_TAG,
                center
            )
        }
    }

    private fun getPointsOfInterest(tile: Tile, mapDataStore: MapDataStore): List<PointOfInterest> {
        val parent = getFullZoomTile(tile)
        synchronized(boundaryLabelNodes) {
            return boundaryLabelNodes.getOrPut(parent) {
                val namedItems = mapDataStore.readNamedItems(parent.aboveLeft, parent.belowRight)
                createPointsOfInterestFromWays(namedItems?.ways ?: emptyList())
                    .distinctBy { it.position.latitude.roundPlaces(4) to it.position.longitude.roundPlaces(4) }
            }
        }
    }

    private fun getFullZoomTile(tile: Tile): Tile {
        var parent = tile
        while (parent.zoomLevel > fullAreaZoomLevel) {
            parent = parent.getParent()
        }
        return parent
    }

    private fun getWayClusters(ways: List<Way>): List<List<Way>> {
        return ways
            .filter { shouldProcess(it) }
            .groupBy { it.tags.sortedBy { tag -> tag.key } }
            .flatMap { ConnectedWayClusterFinder.findClusters(it.value) }
    }

    companion object {
        private val AREA_LABEL_TAG = Tag("trail_sense_area_label", "yes")
    }
}
