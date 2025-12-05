package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor

interface ILineStringRenderer : IGeoJsonFeatureRenderer {
    fun setBackgroundColor(color: PathBackgroundColor)
    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean)
    fun setShouldRenderSmoothPaths(shouldRenderSmoothPaths: Boolean)
    fun setShouldRenderLabels(shouldRenderLabels: Boolean)
}