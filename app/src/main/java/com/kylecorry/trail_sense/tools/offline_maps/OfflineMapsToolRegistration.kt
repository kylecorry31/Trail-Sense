package com.kylecorry.trail_sense.tools.offline_maps

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapFileService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.OfflineMapTileSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object OfflineMapsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.OFFLINE_MAPS,
            context.getString(R.string.offline_maps),
            R.drawable.offline_maps,
            R.id.offlineMapListFragment,
            ToolCategory.Location,
            singletons = listOf(
                { OfflineMapFileRepo.getInstance() },
                { OfflineMapFileService() }
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    OfflineMapTileSource.SOURCE_ID,
                    context.getString(R.string.offline_maps),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_offline_maps_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "offline_maps",
                            title = context.getString(R.string.manage_maps),
                            type = MapLayerPreferenceType.Label,
                            navActionOnClick = R.id.offlineMapListFragment
                        )
                    ),
                    tileSource = ::OfflineMapTileSource,
                    attributionLoader = {
                        val attributions = OfflineMapFileRepo.getInstance()
                            .getAllSync()
                            .filter { it.visible }
                            .mapNotNull { it.attribution?.trim()?.takeIf { attribution -> attribution.isNotBlank() } }
                            .distinct()

                        if (attributions.isEmpty()) {
                            null
                        } else {
                            MapLayerAttribution(
                                attributions.joinToString("\n"),
                                alwaysShow = true
                            )
                        }
                    }
                )
            )
        )
    }
}
