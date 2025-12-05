package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class ContourLayer(taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    GeoJsonLayer<ContourGeoJsonSource>(ContourGeoJsonSource(), taskRunner) {

    fun setPreferences(prefs: ContourMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        renderer.configureLineStringRenderer(shouldRenderLabels = prefs.showLabels.get())
        // TODO: More experimentation required before this is enabled for everyone
//        renderer.configureLineStringRenderer(shouldRenderSmoothPaths = isDebug())
        source.colorScale =
            ElevationColorMapFactory().getElevationColorMap(prefs.colorStrategy.get())
        invalidate()
    }
}