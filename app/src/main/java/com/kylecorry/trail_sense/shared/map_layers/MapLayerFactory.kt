package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer

class MapLayerFactory {

    fun create(
        layerId: String,
        context: Context,
        taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()
    ): ILayer? {
        val registry = AppServiceRegistry.get<MapLayerRegistry>()
        // This is where it would check the layer ID to see if it is a plugin
        return registry.getLayerDefinition(layerId)?.create(context, taskRunner)
    }

}