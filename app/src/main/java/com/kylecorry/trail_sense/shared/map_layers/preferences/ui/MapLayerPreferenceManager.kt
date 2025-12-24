package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getPreferenceValues
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.writePreferenceValues
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters.MapLayerViewPreferenceConverterFactory
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.photo_maps.PhotoMapsToolRegistration

class MapLayerPreferenceManager(
    private val mapId: String,
    private val layers: List<MapLayerDefinition>,
    private val alwaysEnabledLayerIds: List<String>
) {

    private val prefs = AppServiceRegistry.get<PreferencesSubsystem>().preferences

    fun populatePreferences(screen: PreferenceScreen, context: Context) {
        val factory = MapLayerViewPreferenceConverterFactory()
        layers.forEachIndexed { index, layer ->
            if (index > 0) {
                val divider = Preference(context)
                divider.layoutResource = R.layout.preference_divider
                divider.isSelectable = false
                screen.addPreference(divider)
            }

            val header = LayerHeaderPreference(context)
            header.title = layer.name
            screen.addPreference(header)

            val category = createCategory(context)
            category.isVisible = false
            screen.addPreference(category)

            header.onPreferenceClickListener = {
                header.isExpanded = !header.isExpanded
                category.isVisible = header.isExpanded
                true
            }

            val isAlwaysEnabled = alwaysEnabledLayerIds.contains(layer.id)
            val base = DefaultMapLayerDefinitions.getBasePreferences(
                context,
                context.getString(R.string.visible)
            )
            val preferences = base + layer.preferences
            val viewPreferences = preferences.map {
                it to factory.getConverter(it.type).convert(context, mapId, layer.id, it)
            }

            viewPreferences.forEach {
                val preference = it.second
                if (isAlwaysEnabled && it.first.id == DefaultMapLayerDefinitions.ENABLED) {
                    preference.isVisible = false
                }

                if (it.first.id == DefaultMapLayerDefinitions.ENABLED) {

                    header.isLayerEnabled = if (isAlwaysEnabled) {
                        preference.isVisible = true
                        true
                    } else {
                        val key = it.first.getFullPreferenceKey(mapId, layer.id)
                        prefs.getBoolean(key) ?: (it.first.defaultValue as? Boolean ?: true)
                    }

                    preference.onPreferenceChangeListener = { pref, newValue ->
                        header.isLayerEnabled = newValue as Boolean
                        true
                    }
                }

                category.addPreference(preference)

                val dependency = it.first.getDependencyBasePreferenceKey(layer.id)
                if (dependency != null) {
                    preference.dependency = "pref_${mapId}_$dependency"
                }
            }

            val mapIdToName = mapOf(
                MapToolRegistration.MAP_ID to context.getString(R.string.map),
                NavigationToolRegistration.MAP_ID to context.getString(R.string.navigation),
                PhotoMapsToolRegistration.MAP_ID to context.getString(R.string.photo_maps)
            )

            val copyPreference = Preference(context)
            copyPreference.title = context.getString(R.string.copy_settings_to_other_maps)
            copyPreference.isIconSpaceReserved = false
            copyPreference.isSingleLineTitle = false
            copyPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val otherMaps = getOtherMapIds()
                Pickers.items(
                    context,
                    context.getString(R.string.copy_settings_to_other_maps),
                    otherMaps.mapNotNull { mapIdToName[it] },
                    otherMaps.indices.toList()
                ) { indices ->
                    if (indices == null || indices.isEmpty()) {
                        return@items
                    }

                    val bundle = layer.getPreferenceValues(context, mapId)
                    indices.forEach { index ->
                        layer.writePreferenceValues(context, bundle, otherMaps[index])
                    }
                    Alerts.toast(context, context.getString(R.string.settings_copied))
                }
                true
            }
            category.addPreference(copyPreference)
        }
    }

    private fun getOtherMapIds(): List<String> {
        val mapIds = listOf(
            MapToolRegistration.MAP_ID,
            NavigationToolRegistration.MAP_ID,
            PhotoMapsToolRegistration.MAP_ID
        )
        return mapIds.filter { it != mapId }
    }

    private fun createCategory(context: Context): PreferenceCategory {
        val category = PreferenceCategory(context)
        category.isSingleLineTitle = false
        category.isIconSpaceReserved = false
        return category
    }
}
