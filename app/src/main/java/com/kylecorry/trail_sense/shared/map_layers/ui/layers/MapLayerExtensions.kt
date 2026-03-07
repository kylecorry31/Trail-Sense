package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.os.Bundle

object MapLayerParams {
    const val PARAM_TIME = "time"
    const val PARAM_PREFERENCES = "preferences"
    const val PARAM_FEATURE_ID = "feature_id"
    const val PARAM_IS_WIDGET = "is_widget"
}

fun Bundle.getPreferences(): Bundle {
    return getBundle(MapLayerParams.PARAM_PREFERENCES)
        ?: Bundle()
}
