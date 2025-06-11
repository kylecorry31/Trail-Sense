package com.kylecorry.trail_sense.tools.maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

class PhotoMapTileSourceSelector(maps: List<PhotoMap>, private val maxLayers: Int = 4) {

    private val sortedMaps = maps
        .filter { it.isCalibrated && it.visible }
        .sortedBy { it.distancePerPixel() }

    fun getSources(bounds: CoordinateBounds): List<PhotoMap> {
        val minArea = bounds.width().meters().distance.toDouble() * bounds.height()
            .meters().distance.toDouble() * 0.25

        val possibleMaps = sortedMaps.filter {
            val boundary = it.boundary() ?: return@filter false
            val area = boundary.width().meters().distance.toDouble() *
                    boundary.height().meters().distance.toDouble()
            area >= minArea
        }

        val firstContainedIndex = possibleMaps.indexOfFirst {
            contains(
                it.boundary() ?: return@indexOfFirst false,
                bounds,
                fullyContained = true
            )
        }

        val firstContained = if (firstContainedIndex != -1) {
            possibleMaps[firstContainedIndex]
        } else {
            null
        }

        // TODO: This can be merged with the no containing map case
        val intersectsBeforeContained = possibleMaps
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
            intersectsBeforeContained.take(maxLayers - 1) +
                    listOf(firstContained)
        } else {
            possibleMaps.filter {
                contains(
                    it.boundary() ?: return@filter false,
                    bounds
                )
            }.take(maxLayers)
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