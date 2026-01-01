package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.os.Bundle
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.preferences.Preference
import com.kylecorry.andromeda.preferences.PreferenceType

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

fun MapLayerPreference.getFullDependencyPreferenceKey(mapId: String, layerId: String): String? {
    if (dependency == null) {
        return null
    }
    return getPreferenceKey(mapId, layerId, dependency)
}

fun MapLayerPreference.getFullPreferenceKey(mapId: String, layerId: String): String {
    return getPreferenceKey(mapId, layerId, id)
}


private fun getPreferenceKey(mapId: String, layerId: String, preferenceId: String): String {
    return "pref_${mapId}_${layerId}_layer_${preferenceId}"
}

fun IPreferences.getLayerPreferences(
    mapId: String,
    layerIds: List<String>
): Map<String, List<Preference>> {
    val all = getAll()
    val preferences = mutableMapOf<String, List<Preference>>()
    for (layerId in layerIds) {
        val prefix = getPreferenceKey(mapId, layerId, "")
        preferences[layerId] = all.filter { it.key.startsWith(prefix) }
            .map { it.copy(key = it.key.replaceFirst(prefix, "")) }
    }
    return preferences
}

fun IPreferences.getLayerPreferencesBundle(
    mapId: String,
    layerIds: List<String>
): Map<String, Bundle> {
    val preferences = getLayerPreferences(mapId, layerIds)
    val bundles = mutableMapOf<String, Bundle>()
    for (layerId in layerIds) {
        val layerPreferences = preferences[layerId] ?: emptyList()
        val bundle = Bundle()
        for (preference in layerPreferences) {
            val key = preference.key
            when (preference.type) {
                PreferenceType.Int -> bundle.putInt(key, preference.value as Int)
                PreferenceType.Boolean -> bundle.putBoolean(key, preference.value as Boolean)
                PreferenceType.String -> bundle.putString(key, preference.value as String)
                PreferenceType.Float -> bundle.putFloat(key, preference.value as Float)
                PreferenceType.Long -> bundle.putLong(key, preference.value as Long)
            }
        }
        bundles[layerId] = bundle
    }
    return bundles
}

fun IPreferences.copyLayerPreferences(
    layerId: String,
    fromMapId: String,
    toMapIds: List<String>
) {
    // Clear existing preferences for the destination
    for (mapId in toMapIds) {
        val toPreferences = getLayerPreferences(mapId, listOf(layerId))[layerId] ?: emptyList()
        for (preference in toPreferences) {
            remove(getPreferenceKey(mapId, layerId, preference.key))
        }
    }

    // Copy over the new preferences
    val fromPreferences = getLayerPreferences(fromMapId, listOf(layerId))[layerId] ?: emptyList()
    for (mapId in toMapIds) {
        val newPreferences =
            fromPreferences.map { it.copy(key = getPreferenceKey(mapId, layerId, it.key)) }
        putAll(newPreferences)
    }
}