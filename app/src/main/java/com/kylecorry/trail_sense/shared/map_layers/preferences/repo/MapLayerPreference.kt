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
    // TODO: This isn't the right thing to do
    val openDemSettingsOnClick: Boolean = false
)

fun MapLayerPreference.getDependencyBasePreferenceKey(layerId: String): String? {
    if (dependency == null) {
        return null
    }
    return "${layerId}_layer_${dependency}"
}

fun MapLayerPreference.getBasePreferenceKey(layerId: String): String {
    return "${layerId}_layer_${id}"
}

fun MapLayerPreference.getFullPreferenceKey(mapId: String, layerId: String): String {
    return "pref_${mapId}_${getBasePreferenceKey(layerId)}"
}