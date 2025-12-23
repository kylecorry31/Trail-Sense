package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getPreferenceValues
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.writePreferenceValues
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters.MapLayerViewPreferenceConverterFactory
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.photo_maps.PhotoMapsToolRegistration

class MapLayerPreferenceManager(
    private val mapId: String,
    private val layers: List<MapLayerDefinition>,
    private val alwaysEnabledLayerIds: List<String>
) {

    fun populatePreferences(screen: PreferenceScreen, context: Context) {
        val factory = MapLayerViewPreferenceConverterFactory()
        layers.forEach { layer ->
            val category = createCategory(context)
            screen.addPreference(category)

            val base = DefaultMapLayerDefinitions.getBasePreferences(
                context,
                layer.name,
                alwaysEnabledLayerIds.contains(layer.id)
            )
            val preferences = base + layer.preferences
            val viewPreferences = preferences.map {
                factory.getConverter(it.type).convert(it, layer.id)
            }

            viewPreferences.forEach {
                val preference = it.create(context, mapId)
                category.addPreference(preference)
                preference.dependency = if (it.dependency != null) {
                    "pref_${mapId}_${it.dependency}"
                } else {
                    null
                }
            }

            val mapIdToName = mapOf(
                MapToolRegistration.MAP_ID to context.getString(R.string.map),
                NavigationToolRegistration.MAP_ID to context.getString(R.string.navigation),
                PhotoMapsToolRegistration.MAP_ID to context.getString(R.string.photo_maps)
            )

            val copyPreference =
                LabelMapLayerPreference(
                    context.getString(R.string.copy_settings_to_other_maps)
                ) {
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
                }
            category.addPreference(copyPreference.create(context, mapId))
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
