package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

import android.content.Context
import androidx.preference.Preference

interface MapLayerPreference {
    fun create(context: Context, mapId: String): Preference

    val dependency: String?
        get() = null
}