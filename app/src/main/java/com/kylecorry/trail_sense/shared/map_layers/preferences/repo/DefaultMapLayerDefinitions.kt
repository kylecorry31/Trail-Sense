package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import com.kylecorry.trail_sense.R

object DefaultMapLayerDefinitions {
    fun getBasePreferences(
        context: Context,
        layerName: String
    ): List<MapLayerPreference> {
        return listOf(
            MapLayerPreference(
                ENABLED,
                layerName,
                MapLayerPreferenceType.Switch,
                defaultValue = true,
                dependency = null
            ),
            MapLayerPreference(
                OPACITY,
                context.getString(R.string.opacity),
                MapLayerPreferenceType.Seekbar,
                defaultValue = 100
            )
        )
    }

    const val ENABLED = "enabled"
    const val OPACITY = "opacity"
    const val DEFAULT_OPACITY = 100
    const val BACKGROUND_COLOR = "background_color"
    const val SHOW_LABELS = "show_labels"
}
