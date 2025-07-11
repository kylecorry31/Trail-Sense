package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class ContourMapLayerPreferences(
    context: Context,
    mapId: String,
    isEnabledByDefault: Boolean = false
) : PreferenceRepo(context) {

    val isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_contour_layer_enabled",
        isEnabledByDefault
    )

    val opacity by IntPreference(
        cache,
        "pref_${mapId}_contour_layer_opacity",
        50 // percent
    )

    val showLabels by BooleanPreference(
        cache,
        "pref_${mapId}_contour_layer_show_labels",
        true
    )

    val colorWithElevation by BooleanPreference(
        cache,
        "pref_${mapId}_contour_layer_color_with_elevation",
        false
    )
}