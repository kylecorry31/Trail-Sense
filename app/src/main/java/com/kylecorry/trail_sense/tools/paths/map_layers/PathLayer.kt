package com.kylecorry.trail_sense.tools.paths.map_layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.ui.IPathLayer

class PathLayer : IAsyncLayer, IPathLayer {

    private val geoJsonLayer = GeoJsonLayer()

    fun setPreferences(prefs: PathMapLayerPreferences) {
        _percentOpacity = prefs.opacity.get() / 100f
        geoJsonLayer.configureLineStringRenderer(backgroundColor = prefs.backgroundColor.get())
    }

    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        geoJsonLayer.configureLineStringRenderer(shouldRenderWithDrawLines = shouldRenderWithDrawLines)
    }

    fun setShouldRenderSmoothPaths(shouldRenderSmoothPaths: Boolean) {
        geoJsonLayer.configureLineStringRenderer(shouldRenderSmoothPaths = shouldRenderSmoothPaths)
    }

    fun setShouldRenderLabels(shouldRenderLabels: Boolean) {
        geoJsonLayer.configureLineStringRenderer(shouldRenderLabels = shouldRenderLabels)
    }

    override fun setPaths(paths: List<IMappablePath>) {
        geoJsonLayer.setGeoJsonObject(GeoJsonFeatureCollection(paths.map {
            GeoJsonFeature.lineString(
                it.points.map { point -> point.coordinate },
                it.id,
                it.name,
                it.style,
                it.color,
                it.thicknessScale
            )
        }))
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        geoJsonLayer.draw(drawer, map)
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        geoJsonLayer.drawOverlay(drawer, map)
    }

    override fun invalidate() {
        geoJsonLayer.invalidate()
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return geoJsonLayer.onClick(drawer, map, pixel)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        geoJsonLayer.setHasUpdateListener(listener)
    }

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity
}