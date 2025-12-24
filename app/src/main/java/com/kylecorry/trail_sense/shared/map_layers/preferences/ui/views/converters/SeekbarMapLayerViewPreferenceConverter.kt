package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey

class SeekbarMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(
        context: Context,
        mapId: String,
        layerId: String,
        preference: MapLayerPreference
    ): Preference {
        val seekBarPreference = SeekBarPreference(context)
        seekBarPreference.min = preference.min?.toInt() ?: 0
        seekBarPreference.max = preference.max?.toInt() ?: 100
        seekBarPreference.setDefaultValue((preference.defaultValue as? Int) ?: 0)
        seekBarPreference.isIconSpaceReserved = false
        seekBarPreference.key = preference.getFullPreferenceKey(mapId, layerId)
        seekBarPreference.isSingleLineTitle = false
        seekBarPreference.title = preference.title
        seekBarPreference.showSeekBarValue = true
        seekBarPreference.seekBarIncrement = 1
        return seekBarPreference
    }
}