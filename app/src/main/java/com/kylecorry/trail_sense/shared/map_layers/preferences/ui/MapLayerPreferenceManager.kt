package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import android.content.Context
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferences

class MapLayerPreferenceManager(
    private val mapId: String,
    private val layers: List<MapLayerPreferences>,
    private val layerDependency: String? = null
) {

    fun populatePreferences(screen: PreferenceScreen) {
        val context = screen.context

        layers.forEach { layer ->
            val category = createCategory(context, layer.title)
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

    private fun createCategory(context: Context, title: String): PreferenceCategory {
        val category = PreferenceCategory(context)
        category.title = title
        category.isSingleLineTitle = false
        category.isIconSpaceReserved = false
        return category
    }
}