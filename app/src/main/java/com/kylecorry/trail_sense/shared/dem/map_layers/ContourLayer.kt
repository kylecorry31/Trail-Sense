package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.withId

class ContourLayer(taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    GeoJsonLayer<ContourGeoJsonSource>(
        ContourGeoJsonSource(),
        taskRunner = taskRunner,
        minZoomLevel = 13,
        layerId = LAYER_ID
    ) {

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        renderer.configureLineStringRenderer(
            shouldRenderLabels = preferences.getBoolean(SHOW_LABELS, DEFAULT_SHOW_LABELS)
        )
        // TODO: More experimentation required before this is enabled for everyone
//        renderer.configureLineStringRenderer(shouldRenderSmoothPaths = isDebug())
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        source.colorScale = ElevationColorMapFactory().getElevationColorMap(
            ElevationColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
    }

    companion object {
        const val LAYER_ID = "contour"
        const val SHOW_LABELS = "show_labels"
        const val DEFAULT_SHOW_LABELS = true
        const val COLOR = "color"
        val DEFAULT_COLOR = ElevationColorStrategy.Brown
    }
}