package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.andromeda.core.units.PercentBounds
import com.kylecorry.andromeda.core.units.PercentCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.andromeda_temp.CorrectPerspective2
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class FullRegionMapTileLoader(
    private val fullBounds: CoordinateBounds,
    private val outputSize: Size? = null
) {
    private var fullImage: Bitmap? = null
    private var isStopped = false
    private val lock = Mutex()

    suspend fun close() = lock.withLock {
        isStopped = true
        fullImage?.recycle()
        fullImage = null
    }

    suspend fun load(tile: Tile): Bitmap? {
        val fullImage = lock.withLock {
            if (fullImage == null && !isStopped) {
                fullImage = loadFullImage(fullBounds, tile.z)
            }
            fullImage
        } ?: return null

        val bounds = tile.getBounds()

        val leftPercent = (bounds.west - fullBounds.west) / fullBounds.widthDegrees()
        val rightPercent = (bounds.east - fullBounds.west) / fullBounds.widthDegrees()
        val topPercent = (bounds.north - fullBounds.south) / fullBounds.heightDegrees()
        val bottomPercent = (bounds.south - fullBounds.south) / fullBounds.heightDegrees()
        val percentBottomLeft =
            PercentCoordinate(leftPercent.toFloat(), bottomPercent.toFloat())
        val percentBottomRight =
            PercentCoordinate(rightPercent.toFloat(), bottomPercent.toFloat())
        val percentTopLeft = PercentCoordinate(leftPercent.toFloat(), topPercent.toFloat())
        val percentTopRight = PercentCoordinate(rightPercent.toFloat(), topPercent.toFloat())

        return lock.withLock {
            if (fullImage.isRecycled) {
                return null
            }
            fullImage.applyOperationsOrNull(
                CorrectPerspective2(
                    // Bounds are inverted on the Y axis from android's pixel coordinate system
                    PercentBounds(
                        percentBottomLeft,
                        percentBottomRight,
                        percentTopLeft,
                        percentTopRight
                    ),
                    outputSize = outputSize
                ),
                recycleOriginal = false,
            )
        }
    }

    abstract suspend fun loadFullImage(bounds: CoordinateBounds, zoomLevel: Int): Bitmap?

}