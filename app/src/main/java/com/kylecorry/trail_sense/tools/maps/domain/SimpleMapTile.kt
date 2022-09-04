package com.kylecorry.trail_sense.tools.maps.domain

import android.graphics.Rect
import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds

/**
 * All tiles are 256 x 256 pixels
 * @param zoom the zoom level
 * @param bounds the boundary of the tile
 * @param source the source boundary in pixels (always square, may be out of bounds)
 */
data class SimpleMapTile(
    val zoom: Int,
    val bounds: CoordinateBounds,
    val source: Rect
) {

    val size = Size(256, 256)

    val subtiles: List<SimpleMapTile>
        get() {
            val center = bounds.center
            val centerSource = source.centerX() to source.centerY()
            return listOf(
                // Top left
                SimpleMapTile(
                    zoom + 1,
                    CoordinateBounds(
                        bounds.north,
                        center.longitude,
                        center.latitude,
                        bounds.west
                    ),
                    Rect(source.left, source.top, centerSource.first, centerSource.second)
                ),
                // Top right
                SimpleMapTile(
                    zoom + 1,
                    CoordinateBounds(
                        bounds.north,
                        bounds.east,
                        center.latitude,
                        center.longitude
                    ),
                    Rect(centerSource.first, source.top, source.right, centerSource.second)
                ),
                // Bottom left
                SimpleMapTile(
                    zoom + 1,
                    CoordinateBounds(
                        center.latitude,
                        center.longitude,
                        bounds.south,
                        bounds.west
                    ),
                    Rect(source.left, centerSource.second, centerSource.first, source.bottom)
                ),
                // Bottom right
                SimpleMapTile(
                    zoom + 1,
                    CoordinateBounds(
                        center.latitude,
                        bounds.east,
                        bounds.south,
                        center.longitude
                    ),
                    Rect(centerSource.first, centerSource.second, source.right, source.bottom)
                )
            )
        }
}