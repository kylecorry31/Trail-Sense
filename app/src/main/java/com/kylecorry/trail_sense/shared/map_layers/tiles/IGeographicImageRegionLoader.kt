package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds

interface IGeographicImageRegionLoader {

    suspend fun load(tile: Tile): Bitmap?
}