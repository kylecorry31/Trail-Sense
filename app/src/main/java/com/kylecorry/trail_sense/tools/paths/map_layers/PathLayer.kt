package com.kylecorry.trail_sense.tools.paths.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath

class PathLayer : GeoJsonLayer<PathGeoJsonSource>(PathGeoJsonSource()) {

    fun setPreferences(prefs: PathMapLayerPreferences) {
        percentOpacity = prefs.opacity.get() / 100f
        renderer.configureLineStringRenderer(backgroundColor = prefs.backgroundColor.get())
    }

    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        renderer.configureLineStringRenderer(shouldRenderWithDrawLines = shouldRenderWithDrawLines)
    }

    fun reload() {
        source.reload()
        invalidate()
    }
}