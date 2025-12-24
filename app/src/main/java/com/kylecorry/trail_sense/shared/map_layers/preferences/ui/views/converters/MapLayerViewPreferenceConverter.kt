package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import android.content.Context
import androidx.preference.Preference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference

interface MapLayerViewPreferenceConverter {
    fun convert(
        context: Context,
        mapId: String,
        layerId: String,
        preference: MapLayerPreference
    ): Preference
}