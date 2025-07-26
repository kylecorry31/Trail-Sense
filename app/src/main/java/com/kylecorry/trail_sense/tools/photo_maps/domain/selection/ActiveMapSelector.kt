package com.kylecorry.trail_sense.tools.photo_maps.domain.selection

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import kotlin.math.abs

class ActiveMapSelector {

    fun getActiveMap(
        maps: List<PhotoMap>,
        location: Coordinate,
        destination: Coordinate? = null
    ): PhotoMap? {
        val activeMaps = maps
            .filter { contains(it, location) }
            .sortedBy { metersPerPixel(it) }

        val mostZoomedIn = activeMaps.firstOrNull() ?: return null
        val similarZoomLevelMaps = activeMaps.filter {
            isSimilarZoom(it, mostZoomedIn, ZOOM_PCT)
        }

        val activeMap = if (similarZoomLevelMaps.size == 1) {
            mostZoomedIn
        } else {
            getClosestToCenter(similarZoomLevelMaps, location)
        }

        if (destination == null || activeMap?.let { contains(it, destination) } == true) {
            return activeMap
        }

        // Try to find a map that contains the destination and the user
        val navMaps = activeMaps.filter {
            contains(it, destination) && isSimilarZoom(it, mostZoomedIn, NAVIGATION_PCT)
        }

        return getClosestToCenter(navMaps, location) ?: activeMap
    }

    private fun metersPerPixel(map: PhotoMap): Float {
        return map.distancePerPixel()?.meters()?.distance ?: Float.MAX_VALUE
    }

    private fun contains(map: PhotoMap, location: Coordinate): Boolean {
        return map.boundary()?.contains(location) == true
    }

    private fun isSimilarZoom(map: PhotoMap, base: PhotoMap, pct: Float): Boolean {
        return metersPerPixel(map) <= metersPerPixel(base) * (1f + pct)
    }

    private fun getClosestToCenter(
        maps: List<PhotoMap>,
        location: Coordinate
    ): PhotoMap? {
        return maps.minByOrNull {
            val pixel = it.baseProjection.toPixels(location)
            val xPercent = pixel.x / it.metadata.size.width
            val yPercent = pixel.y / it.metadata.size.height
            val xDist = abs(0.5f - xPercent)
            val yDist = abs(0.5f - yPercent)
            xDist + yDist
        }
    }

    companion object {
        private const val ZOOM_PCT = 0.05f
        private const val NAVIGATION_PCT = 0.3f
    }

}