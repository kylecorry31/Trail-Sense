package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.PhotoMapMapLayerPreferences

class MapPreferences(context: Context) : PreferenceRepo(context) {

    val keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        context.getString(R.string.pref_map_keep_unlocked),
        false
    )

    // Layers
    val photoMapLayer = PhotoMapMapLayerPreferences(context, "map", defaultOpacity = 100)
    val contourLayer = ContourMapLayerPreferences(context, "map", isEnabledByDefault = true)
}