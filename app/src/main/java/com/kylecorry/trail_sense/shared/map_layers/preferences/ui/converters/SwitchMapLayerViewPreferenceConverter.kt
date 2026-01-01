package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.converters

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey

class SwitchMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(
        context: Context,
        mapId: String,
        layerId: String,
        preference: MapLayerPreference
    ): Preference {
        val visible = SwitchPreferenceCompat(context)
        visible.setDefaultValue((preference.defaultValue as? Boolean) ?: true)
        visible.isIconSpaceReserved = false
        visible.key = preference.getFullPreferenceKey(mapId, layerId)
        visible.isSingleLineTitle = false
        visible.title = preference.title
        visible.summary = preference.summary
        return visible
    }
}