package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition

class MapLayerDefinitionLoader {

    suspend fun load(context: Context): Map<String, MapLayerDefinition> {
        // TODO: This is where it would query the plugins
        val registry = AppServiceRegistry.get<MapLayerRegistry>()
        return registry.getLayers()
    }

}