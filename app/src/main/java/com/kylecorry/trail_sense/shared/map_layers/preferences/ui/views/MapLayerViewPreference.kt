package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views

import android.content.Context
import androidx.preference.Preference

interface MapLayerViewPreference {
    fun create(context: Context, mapId: String): Preference

    val dependency: String?
        get() = null
}