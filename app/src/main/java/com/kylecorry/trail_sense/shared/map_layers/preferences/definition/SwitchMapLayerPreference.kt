package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

class SwitchMapLayerPreference(
    private val title: String,
    private val key: String,
    private val defaultValue: Boolean = true,
    override val dependency: String? = null,
    private val summary: String? = null
) :
    MapLayerPreference {
    override fun create(context: Context, mapId: String): Preference {
        val visible = SwitchPreferenceCompat(context)
        visible.setDefaultValue(defaultValue)
        visible.isIconSpaceReserved = false
        visible.key = "pref_${mapId}_${key}"
        visible.isSingleLineTitle = false
        visible.title = title
        visible.summary = summary
        return visible
    }
}