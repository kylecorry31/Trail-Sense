package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem


class MapLayerDefinition(
    val id: String,
    val name: String,
    val preferences: List<MapLayerPreference> = emptyList()
)

fun MapLayerDefinition.getPreferenceValues(context: Context, mapId: String): Bundle {
    val cache = AppServiceRegistry.get<PreferencesSubsystem>().preferences
    val bundle = Bundle()

    val preferencesWithBase =
        DefaultMapLayerDefinitions.getBasePreferences(context, name) + preferences

    for (preference in preferencesWithBase) {
        val key = preference.getFullPreferenceKey(mapId, id)
        // TODO: Extract converters
        when (preference.type) {
            MapLayerPreferenceType.Label -> {}
            MapLayerPreferenceType.Enum -> {
                val value = cache.getString(key)
                bundle.putString(preference.id, value ?: (preference.defaultValue as? String))
            }

            MapLayerPreferenceType.Seekbar -> {
                val value = cache.getInt(key)
                bundle.putInt(preference.id, value ?: (preference.defaultValue as? Int) ?: 0)
            }

            MapLayerPreferenceType.Switch -> {
                val value = cache.getBoolean(key)
                bundle.putBoolean(
                    preference.id,
                    value ?: (preference.defaultValue as? Boolean) ?: false
                )
            }
        }
    }
    return bundle
}

fun MapLayerDefinition.writePreferenceValues(context: Context, bundle: Bundle, mapId: String) {
    val cache = AppServiceRegistry.get<PreferencesSubsystem>().preferences
    val preferencesWithBase =
        DefaultMapLayerDefinitions.getBasePreferences(context, name) + preferences
    for (preference in preferencesWithBase) {
        val key = preference.getFullPreferenceKey(mapId, id)
        // TODO: Extract converters
        when (preference.type) {
            MapLayerPreferenceType.Label -> {}
            MapLayerPreferenceType.Enum -> {
                val value = bundle.getString(preference.id)
                if (value == null) {
                    cache.remove(key)
                } else {
                    cache.putString(key, value)
                }
            }

            MapLayerPreferenceType.Seekbar -> {
                if (!bundle.containsKey(preference.id)) {
                    cache.remove(key)
                } else {
                    val value = bundle.getInt(preference.id)
                    cache.putInt(key, value)
                }
            }

            MapLayerPreferenceType.Switch -> {
                if (!bundle.containsKey(preference.id)) {
                    cache.remove(key)
                } else {
                    val value = bundle.getBoolean(preference.id)
                    cache.putBoolean(key, value)
                }
            }
        }
    }
}

