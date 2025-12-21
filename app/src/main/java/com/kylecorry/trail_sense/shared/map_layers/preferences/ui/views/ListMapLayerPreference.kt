package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.Preference

class ListMapLayerPreference(
    private val title: String,
    private val key: String,
    private val values: List<Pair<String, String>>,
    private val defaultValue: String? = null,
    override val dependency: String? = null
) :
    MapLayerViewPreference {
    override fun create(context: Context, mapId: String): Preference {
        val preference = ListPreference(context)
        preference.isIconSpaceReserved = false
        preference.key = "pref_${mapId}_${key}"
        preference.isSingleLineTitle = false
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        preference.title = title
        preference.entries = values.map { it.first }.toTypedArray()
        preference.entryValues = values.map { it.second }.toTypedArray()
        preference.setDefaultValue(defaultValue)
        return preference
    }
}