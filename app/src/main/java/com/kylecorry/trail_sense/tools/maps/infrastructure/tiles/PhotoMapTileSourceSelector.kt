package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class PhotoMapTileSourceSelector(maps: List<PhotoMap>) {

    private val sortedMaps = maps
        .filter { it.isCalibrated }
        .sortedBy { it.distancePerPixel() }

    fun getSources(bounds: CoordinateBounds): List<PhotoMap> {

        val firstContainedIndex = sortedMaps.indexOfFirst {
            contains(
                it.boundary() ?: return@indexOfFirst false,
                bounds,
                fullyContained = true
            )
        }

        val firstContained = if (firstContainedIndex != -1) {
            sortedMaps[firstContainedIndex]
        } else {
            null
        }

        val intersectsBeforeContained = sortedMaps
            .filterIndexed { index, it ->
                if (index >= firstContainedIndex) {
                    return@filterIndexed false
                }

                contains(
                    it.boundary() ?: return@filterIndexed false,
                    bounds
                )
            }


        return if (firstContained != null) {
            intersectsBeforeContained.take(1) +
                    listOf(firstContained)
        } else {
            sortedMaps.filter {
                contains(
                    it.boundary() ?: return@filter false,
                    bounds
                )
            }
        }
    }

    // TODO: Extract to sol
    private fun contains(
        bounds: CoordinateBounds,
        subBounds: CoordinateBounds,
        fullyContained: Boolean = false
    ): Boolean {

        return if (fullyContained) {
            val corners = listOf(
                bounds.contains(subBounds.northWest),
                bounds.contains(subBounds.northEast),
                bounds.contains(subBounds.southWest),
                bounds.contains(subBounds.southEast),
                bounds.contains(subBounds.center)
            )
            corners.all { it }
        } else {
            bounds.intersects(subBounds)
        }
    }

}