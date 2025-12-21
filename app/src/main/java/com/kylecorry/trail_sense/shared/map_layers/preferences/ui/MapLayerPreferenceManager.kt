package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class MapLayerPreferenceManager(
    val mapId: String,
    val layers: List<BaseMapLayerPreferences>,
) {

    fun populatePreferences(screen: PreferenceScreen, context: Context) {
        layers.forEach { layer ->
            val category = createCategory(context)
            screen.addPreference(category)
            layer.getPreferences().preferences.forEach {
                val preference = it.create(context, mapId)
                category.addPreference(preference)
                preference.dependency = if (it.dependency != null) {
                    "pref_${mapId}_${it.dependency}"
                } else {
                    null
                }
            }
            val copyPreference =
                LabelMapLayerPreference(context.getString(R.string.copy_settings_to_other_maps)) {
                    Alerts.dialog(
                        context, context.getString(R.string.copy_settings_to_other_maps),
                        context.getString(
                            R.string.layer_settings_overwrite_warning,
                            layer.name
                        )
                    ) { cancelled ->
                        if (cancelled) {
                            return@dialog
                        }
                        val managers = getOtherMapLayers(layer.layerId)
                        val bundle = layer.toBundle()
                        managers.forEach {
                            it.fromBundle(bundle)
                        }
                        Alerts.toast(context, context.getString(R.string.settings_copied))
                    }

                }
            category.addPreference(copyPreference.create(context, mapId))
        }
    }

    private fun getOtherMapLayers(layerId: String): List<BaseMapLayerPreferences> {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        // TODO: Have a registry for map layer managers
        val maps = listOf(
            prefs.map.layerManager,
            prefs.photoMaps.layerManager,
            prefs.navigation.layerManager
        )
        return maps
            .filter { it.mapId != mapId }
            .flatMap { it.layers }
            .filter { it.layerId == layerId }
    }

    private fun createCategory(context: Context): PreferenceCategory {
        val category = PreferenceCategory(context)
        category.isSingleLineTitle = false
        category.isIconSpaceReserved = false
        return category
    }
}