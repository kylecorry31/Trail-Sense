package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.map.infrastructure.MapsforgeTileRenderer
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo

class OfflineMapTileSource : TileSource {

    private val renderer = MapsforgeTileRenderer()
    private val repo = getAppService<OfflineMapFileRepo>()

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? {
        return renderer.render(context, repo.getAllSync(), tile)
    }

    override suspend fun cleanup() {
        renderer.clear()
    }

    companion object {
        const val SOURCE_ID = "offline_maps"
    }
}
