package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

import android.content.Context
import androidx.preference.Preference

class LabelMapLayerPreference(
    private val title: CharSequence?,
    private val summary: CharSequence? = null,
    override val dependency: String? = null,
    private val onClick: ((context: Context) -> Unit)? = null,
) : MapLayerViewPreference {
    override fun create(
        context: Context,
        mapId: String
    ): Preference {
        val preference = Preference(context)
        preference.isIconSpaceReserved = false
        preference.isSingleLineTitle = false
        preference.title = title
        preference.summary = summary
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            onClick?.invoke(context)
            true
        }
        return preference
    }

}