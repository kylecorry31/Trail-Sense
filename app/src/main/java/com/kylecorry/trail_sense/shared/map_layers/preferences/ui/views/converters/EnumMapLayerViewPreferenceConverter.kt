package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey

class EnumMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(
        context: Context,
        mapId: String,
        layerId: String,
        preference: MapLayerPreference
    ): Preference {
        val listPreference = ListPreference(context)
        listPreference.isIconSpaceReserved = false
        listPreference.key = preference.getFullPreferenceKey(mapId, layerId)
        listPreference.isSingleLineTitle = false
        listPreference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        listPreference.title = preference.title
        val values = preference.values ?: emptyList()
        listPreference.entries = values.map { it.first }.toTypedArray()
        listPreference.entryValues = values.map { it.second }.toTypedArray()
        listPreference.setDefaultValue(preference.defaultValue as? String?)
        return listPreference
    }
}