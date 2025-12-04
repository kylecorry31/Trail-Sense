package com.kylecorry.trail_sense.tools.paths.map_layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_COLOR
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_LINE_STYLE
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_NAME
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.LineStringLayer
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer

class PathLayer : IAsyncLayer, IPathLayer {

    private val lineStringLayer = LineStringLayer()

    fun setPreferences(prefs: PathMapLayerPreferences) {
        _percentOpacity = prefs.opacity.get() / 100f
        lineStringLayer.setBackgroundColor(prefs.backgroundColor.get())
    }

    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        lineStringLayer.setShouldRenderWithDrawLines(shouldRenderWithDrawLines)
    }

    fun setShouldRenderSmoothPaths(shouldRenderSmoothPaths: Boolean) {
        lineStringLayer.setShouldRenderSmoothPaths(shouldRenderSmoothPaths)
    }

    fun setShouldRenderLabels(shouldRenderLabels: Boolean) {
        lineStringLayer.setShouldRenderLabels(shouldRenderLabels)
    }

    override fun setPaths(paths: List<IMappablePath>) {
        lineStringLayer.setFeatures(paths.map {
            GeoJsonFeature(
                it.id, GeoJsonLineString(it.points.map { point ->
                    GeoJsonPosition(
                        point.coordinate.longitude,
                        point.coordinate.latitude,
                        point.elevation?.toDouble()
                    )
                }), mapOf(
                    GEO_JSON_PROPERTY_NAME to it.name,
                    GEO_JSON_PROPERTY_LINE_STYLE to it.style.id,
                    GEO_JSON_PROPERTY_COLOR to it.color,
                    GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE to it.thicknessScale
                )
            )
        })
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
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

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return lineStringLayer.onClick(drawer, map, pixel)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        lineStringLayer.setHasUpdateListener(listener)
    }

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity
}