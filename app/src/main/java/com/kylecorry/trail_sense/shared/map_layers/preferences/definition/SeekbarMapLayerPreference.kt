package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SeekBarPreference

class SeekbarMapLayerPreference(
    private val title: String,
    private val key: String,
    private val defaultValue: Int = 0,
    private val min: Int = 0,
    private val max: Int = 100,
    override val dependency: String? = null
) : MapLayerPreference {
    override fun create(
        context: Context,
        mapId: String
    ): Preference {
        val seekBarPreference = SeekBarPreference(context)
        seekBarPreference.min = min
        seekBarPreference.max = max
        seekBarPreference.setDefaultValue(defaultValue)
        seekBarPreference.isIconSpaceReserved = false
        seekBarPreference.key = "pref_${mapId}_${key}"
        seekBarPreference.isSingleLineTitle = false
        seekBarPreference.title = title
        seekBarPreference.showSeekBarValue = true
        seekBarPreference.seekBarIncrement = 1
        return seekBarPreference
    }

}