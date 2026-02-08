package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class ContourLayer : GeoJsonLayer<ContourGeoJsonSource>(
    ContourGeoJsonSource(),
    minZoomLevel = 13,
    layerId = ContourGeoJsonSource.SOURCE_ID
) {

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        renderer.configureLineStringRenderer(
            shouldRenderLabels = preferences.getBoolean(
                ContourGeoJsonSource.SHOW_LABELS,
                ContourGeoJsonSource.DEFAULT_SHOW_LABELS
            )
        )
        // TODO: More experimentation required before this is enabled for everyone
//        renderer.configureLineStringRenderer(shouldRenderSmoothPaths = isDebug())
    }

}
