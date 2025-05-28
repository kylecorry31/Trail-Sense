package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class PhotoMapTileSourceSelector(maps: List<PhotoMap>) {

    private val sortedMaps = maps
        .filter { it.isCalibrated }
        .sortedBy { it.distancePerPixel() }

    fun getSources(bounds: CoordinateBounds): List<PhotoMap> {

        val firstContained = sortedMaps.firstOrNull {
            contains(
                it.boundary() ?: return@firstOrNull false,
                bounds,
                fullyContained = true
            )
        }

        val firstIntersect = sortedMaps.firstOrNull {
            contains(
                it.boundary() ?: return@firstOrNull false,
                bounds
            )
        }


        return if (firstContained != null) {
            listOfNotNull(firstIntersect, firstContained).distinct()
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
        val corners = listOf(
            bounds.contains(subBounds.northWest),
            bounds.contains(subBounds.northEast),
            bounds.contains(subBounds.southWest),
            bounds.contains(subBounds.southEast),
            bounds.contains(subBounds.center)
        )

        return if (fullyContained) {
            corners.all { it }
        } else {
            corners.any { it }
        }
    }

}