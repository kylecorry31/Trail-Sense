package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.content.Context
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.bitmaps.BitmapOperation
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PhotoMapTileSourceSelector(
    private val context: Context,
    maps: List<PhotoMap>,
    private val maxLayers: Int = 4,
    private val replaceWhitePixels: Boolean = false,
    private val loadPdfs: Boolean = true,
    private val isPixelPerfect: Boolean = false,
    private val operations: List<BitmapOperation> = emptyList()
) : ITileSourceSelector {

    private val sortedMaps = maps
        .filter { it.isCalibrated && it.visible }
        .sortedBy { it.distancePerPixel() }

    // TODO: Factor in rotation by using projection to see if the bounds intersect/are contained
    override fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader> {
        val minArea = bounds.width().meters().distance.toDouble() * bounds.height()
            .meters().distance.toDouble() * 0.25

        val possibleMaps = sortedMaps.filter {
            val boundary = it.boundary() ?: return@filter false
            if (boundary == CoordinateBounds.world) {
                return@filter true
            }
            val area = boundary.width().meters().distance.toDouble() *
                    boundary.height().meters().distance.toDouble()
            area >= minArea
        }

        val firstContained = possibleMaps.firstOrNull {
            contains(
                it.boundary() ?: return@firstOrNull false,
                bounds,
                fullyContained = true
            )
        }

        val containedMaps = possibleMaps.filter {
            contains(
                it.boundary() ?: return@filter false,
                bounds
            )
        }.take(maxLayers).toMutableList()


        val maps = if (firstContained != null && !containedMaps.contains(firstContained)) {
            if (containedMaps.size == maxLayers) {
                containedMaps.removeLastOrNull()
            }
            containedMaps.add(firstContained)
            containedMaps
        } else if (firstContained != null && SolMath.isZero(
                firstContained.baseRotation() - firstContained.calibration.rotation,
                0.5f
            )
        ) {
            // The contained map isn't really rotated so only include a map after it if replaceWhitePixels is true
            val index = containedMaps.indexOf(firstContained)
            containedMaps.subList(
                0,
                minOf(index + if (replaceWhitePixels) 2 else 1, containedMaps.size)
            )
        } else {
            containedMaps
        }

        return maps.map {
            PhotoMapRegionLoader(
                context,
                it,
                replaceWhitePixels,
                loadPdfs,
                isPixelPerfect,
                operations
            )
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