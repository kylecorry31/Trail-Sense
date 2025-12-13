package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.FullRegionMapTileLoader
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.FullRegionMapTileSource

class HillshadeMapTileSource : FullRegionMapTileSource() {

    private val minZoomLevel = 10
    private val maxZoomLevel = 19
    private val baseResolution = 1 / 240.0
    private val validResolutions = mapOf(
        10 to baseResolution * 8,
        11 to baseResolution * 4,
        12 to baseResolution * 2,
        13 to baseResolution,
        14 to baseResolution / 2,
        15 to baseResolution / 4,
        16 to baseResolution / 4,
        17 to baseResolution / 4,
        18 to baseResolution / 4,
        19 to baseResolution / 4
    )

    override fun getLoader(fullBounds: CoordinateBounds): FullRegionMapTileLoader {
        return object : FullRegionMapTileLoader(fullBounds) {
            override suspend fun loadFullImage(
                bounds: CoordinateBounds,
                zoomLevel: Int
            ): Bitmap? {
                val zoomLevel = zoomLevel.coerceIn(minZoomLevel, maxZoomLevel)
                return DEM.hillshadeImage(bounds, validResolutions[zoomLevel]!!, 3f)
            }

        }
    }
}