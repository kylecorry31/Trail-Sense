package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask2
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlinx.coroutines.CancellationException

abstract class FeatureRenderer : IGeoJsonFeatureRenderer {

    protected var isInvalid = true
    private var features: List<GeoJsonFeature> = emptyList()
    private var updateListener: (() -> Unit)? = null

    private var backgroundAction: (suspend (viewBounds: Rectangle, bounds: CoordinateBounds, mapProjection: IMapViewProjection, features: List<GeoJsonFeature>) -> Unit)? =
        null
    private val taskRunner = MapLayerBackgroundTask2()

    override fun setFeatures(features: List<GeoJsonFeature>) {
        this@FeatureRenderer.features = filterFeatures(features)
        invalidate()
        notifyListeners()
    }

    open fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    protected fun setRunInBackgroundWhenChanged(action: (suspend (viewBounds: Rectangle, bounds: CoordinateBounds, mapProjection: IMapViewProjection, features: List<GeoJsonFeature>) -> Unit)?) {
        backgroundAction = action
    }

    abstract fun draw(drawer: ICanvasDrawer, map: IMapView, features: List<GeoJsonFeature>)

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        // Avoid drawing while in safe mode
        if (SafeMode.isEnabled()) {
            return
        }

        draw(drawer, map, features)

        if (backgroundAction != null) {
            taskRunner.scheduleUpdate(
                    drawer.getBounds(45f), // TODO: Cache this
                map.mapBounds,
                map.mapProjection,
                isInvalid
            ) { viewBounds, bounds, projection ->
                isInvalid = false
                try {
                    backgroundAction?.invoke(viewBounds, bounds, projection, features)
                    updateListener?.invoke()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    e.printStackTrace()
                    isInvalid = true
                }
            }
        }
    }

    override fun invalidate() {
        isInvalid = true
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        // Do nothing by default
        return false
    }
}