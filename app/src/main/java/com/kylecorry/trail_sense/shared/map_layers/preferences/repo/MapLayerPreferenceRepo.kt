package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class MapLayerPreferenceRepo {

    private val prefs = AppServiceRegistry.get<PreferencesSubsystem>()

    fun getActiveLayerIds(mapId: String): List<String> {
        return prefs.preferences.getString("pref_${mapId}_active_layers")?.split(",") ?: emptyList()
    }

    fun setActiveLayerIds(mapId: String, layerIds: List<String>) {
        prefs.preferences.putString("pref_${mapId}_active_layers", layerIds.joinToString(","))
    }

}