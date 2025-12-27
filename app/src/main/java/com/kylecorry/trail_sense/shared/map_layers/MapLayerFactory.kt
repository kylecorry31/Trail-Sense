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
        // TODO: This is where it would check the layer ID to see if it is a plugin and create a plugin layer
        // The plugin layer IDs should use a consistent format like plugin__package_name__layer_id__tile / plugin__package_name__layer_id__feature
        val registry = AppServiceRegistry.get<MapLayerRegistry>()
        return registry.getLayerDefinition(layerId)?.create(context, taskRunner)
    }

}