package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.preferences.Preference
import com.kylecorry.andromeda.preferences.PreferenceType
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.text.LevenshteinDistance

class MapLayerPreferenceRepo {

    private val prefs = AppServiceRegistry.get<PreferencesSubsystem>()
    private val distanceMetric = LevenshteinDistance()

    fun getActiveLayerIds(mapId: String): List<String> {
        return prefs.preferences.getString("pref_${mapId}_active_layers")?.split(",") ?: emptyList()
    }

    fun setActiveLayerIds(mapId: String, layerIds: List<String>) {
        prefs.preferences.putString("pref_${mapId}_active_layers", layerIds.joinToString(","))
    }

    fun getLayerPreferences(
        mapId: String,
        layerIds: List<String>
    ): Map<String, List<Preference>> {
        val all = prefs.preferences.getAll()
        val preferences = mutableMapOf<String, List<Preference>>()
        for (layerId in layerIds) {
            val prefix = getPreferenceKey(mapId, layerId, "")
            preferences[layerId] = all.filter { it.key.startsWith(prefix) }
                .map { it.copy(key = it.key.replaceFirst(prefix, "")) }
        }
        return preferences
    }

    fun getLayerPreferencesBundle(
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

    fun copyLayerPreferences(
        layerId: String,
        fromMapId: String,
        toMapIds: List<String>
    ) {
        val sourceLayerOrder = getActiveLayerIds(fromMapId)

        // Clear existing preferences for the destination
        for (mapId in toMapIds) {
            removeLayerPreferences(mapId, layerId)
        }

        // Copy over the new preferences
        val fromPreferences =
            getLayerPreferences(fromMapId, listOf(layerId))[layerId] ?: emptyList()
        for (mapId in toMapIds) {
            val newPreferences =
                fromPreferences.map { it.copy(key = getPreferenceKey(mapId, layerId, it.key)) }
            prefs.preferences.putAll(newPreferences)

            // Add the layer to the destination's active layers if not present
            val destinationLayers = getActiveLayerIds(mapId)
            if (!destinationLayers.contains(layerId)) {
                val newLayers =
                    insertLayer(layerId, sourceLayerOrder, destinationLayers)
                setActiveLayerIds(mapId, newLayers)
            }
        }
    }

    private fun insertLayer(
        layerId: String,
        sourceOrder: List<String>,
        destinationOrder: List<String>
    ): List<String> {
        val sourceString = sourceOrder.joinToString(",")
        var bestDistance = Int.MAX_VALUE
        var bestOrder = listOf(layerId) + destinationOrder
        for (i in destinationOrder.indices) {
            val newOrder = destinationOrder.toMutableList()
            newOrder.add(i, layerId)
            val newString = newOrder.joinToString(",")
            val distance = distanceMetric.editDistance(sourceString, newString)
            if (distance < bestDistance) {
                bestDistance = distance
                bestOrder = newOrder
            }
        }
        return bestOrder
    }


    fun removeLayerPreferences(mapId: String, layerId: String) {
        val toPreferences = getLayerPreferences(mapId, listOf(layerId))[layerId] ?: emptyList()
        for (preference in toPreferences) {
            prefs.preferences.remove(getPreferenceKey(mapId, layerId, preference.key))
        }
    }

    companion object {
        fun getPreferenceKey(mapId: String, layerId: String, preferenceId: String): String {
            return "pref_${mapId}_${layerId}_layer_${preferenceId}"
        }
    }

}