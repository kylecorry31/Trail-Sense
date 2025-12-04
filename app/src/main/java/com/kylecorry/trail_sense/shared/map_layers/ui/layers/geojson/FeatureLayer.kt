package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import kotlinx.coroutines.CancellationException

abstract class FeatureLayer : IAsyncLayer {

    protected var isInvalid = true
    private var features: List<GeoJsonFeature> = emptyList()
    private var updateListener: (() -> Unit)? = null
    private val taskRunner = MapLayerBackgroundTask()

    fun setFeatures(features: List<GeoJsonFeature>) {
        this@FeatureLayer.features = features
        invalidate()
    }

    open fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    // TODO: Pass drawer bounds?
    abstract suspend fun renderFeaturesInBackground(
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        features: List<GeoJsonFeature>
    )

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        // Avoid drawing while in safe mode
        if (SafeMode.isEnabled()) {
            return
        }

        taskRunner.scheduleUpdate(
            map.mapBounds,
            map.metersPerPixel,
            isInvalid
        ) { bounds, metersPerPixel ->
            isInvalid = false
            try {
                renderFeaturesInBackground(bounds, metersPerPixel, filterFeatures(features))
                updateListener?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                isInvalid = true
            }
        }

        // TODO: task runner w/ abstract render in background method
        // TODO: render features
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        isInvalid = true
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        // Do nothing by default
        return false
    }

    fun setPercentOpacity(opacity: Float) {
        _percentOpacity = opacity.coerceIn(0f, 1f)
    }

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity
}