package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.os.Bundle

object MapLayerParams {
    const val PARAM_TIME = "time"
    const val PARAM_PREFERENCES = "preferences"
}

fun Bundle.getPreferences(): Bundle {
    return getBundle(MapLayerParams.PARAM_PREFERENCES)
        ?: Bundle()
}
