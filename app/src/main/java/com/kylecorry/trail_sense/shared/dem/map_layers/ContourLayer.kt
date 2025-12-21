package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.withId

class ContourLayer(taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    GeoJsonLayer<ContourGeoJsonSource>(
        ContourGeoJsonSource(),
        taskRunner = taskRunner,
        minZoomLevel = 13
    ) {

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        renderer.configureLineStringRenderer(
            shouldRenderLabels = preferences.getBoolean(
                ContourMapLayerPreferences.SHOW_LABELS
            )
        )
        // TODO: More experimentation required before this is enabled for everyone
//        renderer.configureLineStringRenderer(shouldRenderSmoothPaths = isDebug())
        val strategyId = preferences.getLong(ContourMapLayerPreferences.COLOR_STRATEGY_ID)
        source.colorScale = ElevationColorMapFactory().getElevationColorMap(
            ElevationColorStrategy.entries.withId(strategyId) ?: ElevationColorStrategy.Brown
        )
    }
}