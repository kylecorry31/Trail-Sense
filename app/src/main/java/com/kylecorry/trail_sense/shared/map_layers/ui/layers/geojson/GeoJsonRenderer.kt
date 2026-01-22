package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.trail_sense.shared.extensions.normalize
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features.GeoJsonLineStringRenderer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features.GeoJsonPointRenderer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features.GeoJsonPolygonRenderer
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor

class GeoJsonRenderer : IGeoJsonRenderer {

    private val lineStringRenderer = GeoJsonLineStringRenderer()
    private val pointRenderer = GeoJsonPointRenderer()
    private val polygonRenderer = GeoJsonPolygonRenderer()

    fun configureLineStringRenderer(
        backgroundColor: PathBackgroundColor? = null,
        shouldRenderWithDrawLines: Boolean? = null,
        shouldRenderLabels: Boolean? = null,
        shouldRenderSmoothPaths: Boolean? = null,
    ) {
        if (backgroundColor != null) {
            lineStringRenderer.setBackgroundColor(backgroundColor)
        }
        if (shouldRenderWithDrawLines != null) {
            lineStringRenderer.setShouldRenderWithDrawLines(shouldRenderWithDrawLines)
        }
        if (shouldRenderLabels != null) {
            lineStringRenderer.setShouldRenderLabels(shouldRenderLabels)
        }
        if (shouldRenderSmoothPaths != null) {
            lineStringRenderer.setShouldRenderSmoothPaths(shouldRenderSmoothPaths)
        }
    }

    fun configurePointRenderer(
        shouldRenderLabels: Boolean? = null
    ) {
        if (shouldRenderLabels != null) {
            pointRenderer.setShouldRenderLabels(shouldRenderLabels)
        }
    }

    fun setGeoJsonObject(obj: GeoJsonObject) {
        val features = obj.normalize()
        lineStringRenderer.setFeatures(features)
        pointRenderer.setFeatures(features)
        polygonRenderer.setFeatures(features)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        lineStringRenderer.setHasUpdateListener(listener)
        pointRenderer.setHasUpdateListener(listener)
        polygonRenderer.setHasUpdateListener(listener)
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        polygonRenderer.draw(drawer, map)
        lineStringRenderer.draw(drawer, map)
        pointRenderer.draw(drawer, map)
    }

    override fun invalidate() {
        lineStringRenderer.invalidate()
        pointRenderer.invalidate()
        polygonRenderer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return pointRenderer.onClick(drawer, map, pixel) || lineStringRenderer.onClick(
            drawer,
            map,
            pixel
        ) || polygonRenderer.onClick(drawer, map, pixel)
    }

    fun setOnClickListener(listener: (feature: GeoJsonFeature) -> Boolean) {
        // TODO: Handle clicks for other layers
        pointRenderer.setOnClickListener(listener)
    }

    fun setSelectedFeature(featureId: String?) {
        // TODO: Handle selection for other renderers
        pointRenderer.setSelectedFeature(featureId)
    }


}