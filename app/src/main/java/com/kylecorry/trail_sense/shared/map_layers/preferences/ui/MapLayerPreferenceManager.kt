package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences

class MapLayerPreferenceManager(
    private val mapId: String,
    private val layers: List<MapLayerViewPreferences>,
    private val layerDependency: String? = null
) {

    fun populatePreferences(screen: PreferenceScreen, context: Context) {
        layers.forEach { layer ->
            val category = createCategory(context)
            screen.addPreference(category)
            category.dependency = layerDependency
            layer.preferences.forEach {
                val preference = it.create(context, mapId)
                category.addPreference(preference)
                preference.dependency = if (it.dependency != null) {
                    "pref_${mapId}_${it.dependency}"
                } else {
                    null
                }
            }
        }
    }

    private fun createCategory(context: Context): PreferenceCategory {
        val category = PreferenceCategory(context)
        category.isSingleLineTitle = false
        category.isIconSpaceReserved = false
        return category
    }
}