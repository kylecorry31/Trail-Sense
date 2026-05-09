package com.kylecorry.trail_sense.tools.offline_maps.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge.MapsforgeTileRenderer

class MapsforgeTileSource : TileSource {

    private val renderer = MapsforgeTileRenderer()
    private val repo = getAppService<MapRepo>()

    override suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: Bundle
    ): Bitmap? = onDefault {
        val featureId = params.getString(MapLayerParams.PARAM_FEATURE_ID)?.toLongOrNull()
        val highDetailMode = params.getBoolean(MapLayerParams.PARAM_HIGH_DETAIL_MODE, false)
        val maps = if (featureId == null) {
            repo.getVectorMaps().filter { it.visible }
        } else {
            listOfNotNull(repo.getVectorMap(featureId))
        }.filter { it.type == OfflineMapFileType.Mapsforge }
        renderer.render(context, maps, tile, highDetailMode)
    }

    override suspend fun cleanup() {
        renderer.clear()
    }

    companion object {
        const val SOURCE_ID = "offline_maps"
    }
}
