package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

class SwitchMapLayerPreference(
    private val title: String,
    private val key: String,
    private val defaultValue: Boolean = true,
    override val dependency: String? = null
) :
    MapLayerPreference {
    override fun create(context: Context, mapId: String): Preference {
        val visible = SwitchPreferenceCompat(context)
        visible.setDefaultValue(defaultValue)
        visible.isIconSpaceReserved = false
        visible.key = "pref_${mapId}_${key}"
        visible.isSingleLineTitle = false
        visible.title = title
        return visible
    }
}