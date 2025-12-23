package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import com.kylecorry.trail_sense.R

object DefaultMapLayerDefinitions {
    fun getBasePreferences(
        context: Context,
        layerName: String,
        alwaysEnabled: Boolean = false
    ): List<MapLayerPreference> {
        return listOf(
            MapLayerPreference(
                ENABLED,
                layerName,
                if (alwaysEnabled) MapLayerPreferenceType.Label else MapLayerPreferenceType.Switch,
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
}