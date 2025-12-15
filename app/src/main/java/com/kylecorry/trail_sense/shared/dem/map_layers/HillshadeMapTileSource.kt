package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Bitmap
import android.util.Size
import com.kylecorry.andromeda.bitmaps.operations.Convert
import com.kylecorry.andromeda.bitmaps.operations.Resize
import com.kylecorry.andromeda.bitmaps.operations.applyOperationsOrNull
import com.kylecorry.trail_sense.shared.andromeda_temp.Parallel
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileSource

class HillshadeMapTileSource : TileSource {

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

    override suspend fun load(tiles: List<Tile>, onLoaded: suspend (Tile, Bitmap?) -> Unit) {
        Parallel.forEach(tiles) {
            val bitmap = loadTile(it)
            onLoaded(it, bitmap)
        }
    }

    private suspend fun loadTile(tile: Tile): Bitmap? {
        val zoomLevel = tile.z.coerceIn(minZoomLevel, maxZoomLevel)
        return DEM.hillshadeImage(tile.getBounds(), validResolutions[zoomLevel]!!, 3f)
            .applyOperationsOrNull(
                Convert(Bitmap.Config.ARGB_8888),
                Resize(Size(10, 10), true),
                Convert(Bitmap.Config.RGB_565),
            )
    }
}