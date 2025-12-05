package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.extensions.normalize
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor

class GeoJsonLayer : IAsyncLayer {

    private val lineStringLayer: ILineStringRenderer = if (isDebug()) {
        LineStringRenderer()
    } else {
        LegacyLineStringRenderer()
    }

    fun configureLineStringRenderer(
        backgroundColor: PathBackgroundColor? = null,
        shouldRenderWithDrawLines: Boolean? = null,
        shouldRenderLabels: Boolean? = null,
        shouldRenderSmoothPaths: Boolean? = null,
    ) {
        if (backgroundColor != null) {
            lineStringLayer.setBackgroundColor(backgroundColor)
        }
        if (shouldRenderWithDrawLines != null) {
            lineStringLayer.setShouldRenderWithDrawLines(shouldRenderWithDrawLines)
        }
        if (shouldRenderLabels != null) {
            lineStringLayer.setShouldRenderLabels(shouldRenderLabels)
        }
        if (shouldRenderSmoothPaths != null) {
            lineStringLayer.setShouldRenderSmoothPaths(shouldRenderSmoothPaths)
        }
    }

    fun setGeoJsonObject(obj: GeoJsonObject) {
        val features = obj.normalize()
        lineStringLayer.setFeatures(features)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        lineStringLayer.setHasUpdateListener(listener)
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        lineStringLayer.draw(drawer, map)
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        lineStringLayer.drawOverlay(drawer, map)
    }

    override fun invalidate() {
        lineStringLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return lineStringLayer.onClick(drawer, map, pixel)
    }

    override val percentOpacity: Float
        get() = lineStringLayer.percentOpacity
}