package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import kotlinx.coroutines.CancellationException

open class GeoJsonLayer<T : GeoJsonSource>(
    protected val source: T,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()
) : IAsyncLayer {

    protected val renderer = GeoJsonRenderer()
    private var isInvalid = true

    init {
        renderer.setOnClickListener(this::onClick)
        taskRunner.addTask { bounds, metersPerPixel ->
            isInvalid = false
            try {
                val obj =
                    source.load(bounds, metersPerPixel) ?: GeoJsonFeatureCollection(emptyList())
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
            map.mapBounds,
            map.metersPerPixel,
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

    override var percentOpacity: Float = 1f
}