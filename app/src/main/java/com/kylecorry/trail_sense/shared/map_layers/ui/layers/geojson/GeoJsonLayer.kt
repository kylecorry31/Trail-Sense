package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import kotlinx.coroutines.CancellationException

open class GeoJsonLayer<T : GeoJsonSource>(
    protected val source: T,
    private val minZoomLevel: Int? = null,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()
) : IAsyncLayer {
    val renderer = GeoJsonRenderer()
    private var isInvalid = true

    init {
        renderer.setOnClickListener(this::onClick)
        taskRunner.addTask { viewBounds, bounds, projection ->
            isInvalid = false
            try {
                if (minZoomLevel != null) {
                    val zoomLevel = TileMath.getZoomLevel(
                        bounds,
                        projection.metersPerPixel.toDouble()
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
        renderer.setHasUpdateListener(listener)
    }

    override fun draw(
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

    open fun onClick(feature: GeoJsonFeature): Boolean {
        return false
    }

    override fun stop() {
        taskRunner.stop()
    }

    override var percentOpacity: Float = 1f
}