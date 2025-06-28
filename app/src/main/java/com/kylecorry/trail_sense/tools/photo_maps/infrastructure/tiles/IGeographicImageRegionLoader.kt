package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds

interface IGeographicImageRegionLoader {

    suspend fun load(tile: Tile): Bitmap? {
        return load(tile.getBounds(), tile.size)
    }

    suspend fun load(bounds: CoordinateBounds, maxSize: Size): Bitmap?
}