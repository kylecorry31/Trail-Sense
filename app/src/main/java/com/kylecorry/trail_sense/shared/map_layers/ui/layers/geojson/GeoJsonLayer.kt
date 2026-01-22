package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.trail_sense.shared.extensions.isClickable
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CancellationException

typealias OnGeoJsonFeatureClickListener = (GeoJsonFeature) -> Unit

open class GeoJsonLayer<T : GeoJsonSource>(
    protected val source: T,
    private val minZoomLevel: Int? = null,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    override val layerId: String
) : IAsyncLayer {
    val renderer = GeoJsonRenderer()
    private var isInvalid = true
    private var updateListener: (() -> Unit)? = null
    private var onFeatureClick: OnGeoJsonFeatureClickListener? = null

    init {
        renderer.setOnClickListener(this::onClick)
        taskRunner.addTask { viewBounds, bounds, projection ->
            isInvalid = false
            try {
                if (minZoomLevel != null) {
                    val zoomLevel = TileMath.getZoomLevel(
                        bounds,
                        projection.metersPerPixel
                    )

                    if (zoomLevel < minZoomLevel) {
                        renderer.setGeoJsonObject(GeoJsonFeatureCollection(emptyList()))
                        return@addTask
                    }
                }

                val obj =
                    source.load(bounds, projection.metersPerPixel) ?: GeoJsonFeatureCollection(
                        emptyList()
                    )
                renderer.setGeoJsonObject(obj)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                isInvalid = true
            }
        }
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
        renderer.setHasUpdateListener(listener)
    }

    override fun setPreferences(preferences: Bundle) {
        percentOpacity = preferences.getInt(
            DefaultMapLayerDefinitions.OPACITY,
            DefaultMapLayerDefinitions.DEFAULT_OPACITY
        ) / 100f
    }

    override fun draw(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        renderer.draw(drawer, map)
        taskRunner.scheduleUpdate(
            drawer.getBounds(45f),
            map.mapBounds,
            map.mapProjection,
            isInvalid
        )
    }

    override fun drawOverlay(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        isInvalid = true
        renderer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return renderer.onClick(drawer, map, pixel)
    }

    fun setOnFeatureClickListener(listener: OnGeoJsonFeatureClickListener?) {
        onFeatureClick = listener
    }

    open fun onClick(feature: GeoJsonFeature): Boolean {
        if (onFeatureClick == null || !feature.isClickable()) {
            return false
        }
        onFeatureClick?.invoke(feature)
        return true
    }

    override fun start() {
        Tools.subscribe(MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED, this::onSelectionBroadcast)
    }

    override fun stop() {
        Tools.unsubscribe(MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED, this::onSelectionBroadcast)
        taskRunner.stop()
    }

    private fun onSelectionBroadcast(bundle: Bundle): Boolean {
        val broadcastLayerId = bundle.getString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_LAYER_ID)
        if (broadcastLayerId == layerId) {
            val featureId = bundle.getString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_FEATURE_ID)
            renderer.setSelectedFeature(featureId)
        }
        return true
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override var percentOpacity: Float = 1f
}