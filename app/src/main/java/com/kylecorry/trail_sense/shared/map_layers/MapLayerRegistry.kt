package com.kylecorry.trail_sense.shared.map_layers

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition

class MapLayerRegistry {

    private val lock = Any()
    private val layerDefinitions: MutableMap<String, MapLayerDefinition> = mutableMapOf()


    fun register(layer: MapLayerDefinition) {
        synchronized(lock) {
            layerDefinitions[layer.id] = layer
        }
    }

    fun unregister(layerId: String) {
        synchronized(lock) {
            layerDefinitions.remove(layerId)
        }
    }

    fun getLayers(): Map<String, MapLayerDefinition> {
        return synchronized(lock) {
            layerDefinitions.toMap()
        }
    }

}