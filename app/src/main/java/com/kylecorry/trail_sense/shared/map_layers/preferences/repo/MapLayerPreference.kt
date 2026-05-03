package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

data class MapLayerPreference(
    val id: String,
    val title: CharSequence?,
    val type: MapLayerPreferenceType,
    val summary: CharSequence? = null,
    val defaultValue: Any? = null,
    val dependency: String? = DefaultMapLayerDefinitions.ENABLED,
    val min: Number? = null,
    val max: Number? = null,
    val values: List<Pair<String, String>>? = null,
    val navActionOnClick: Int? = null
)

fun MapLayerPreference.getFullDependencyPreferenceKey(mapId: String, layerId: String): String? {
    if (dependency == null) {
        return null
    }
    return MapLayerPreferenceRepo.getPreferenceKey(mapId, layerId, dependency)
}

fun MapLayerPreference.getFullPreferenceKey(mapId: String, layerId: String): String {
    return MapLayerPreferenceRepo.getPreferenceKey(mapId, layerId, id)
}
