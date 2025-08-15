package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences

abstract class BaseMapLayerPreferences(context: Context) : PreferenceRepo(context) {
    abstract fun getPreferences(): MapLayerViewPreferences
}