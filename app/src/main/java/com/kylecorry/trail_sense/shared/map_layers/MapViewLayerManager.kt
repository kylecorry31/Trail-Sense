package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.withLayerOpacity
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IDraggableLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.OnGeoJsonFeatureClickListener
import com.kylecorry.trail_sense.shared.getBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.Instant

class MapViewLayerManager(private val invalidateView: () -> Unit) {

    private var layers = listOf<ILayer>()
    private var onGeoJsonFeatureClickListener: OnGeoJsonFeatureClickListener? = null
    private var dragLayer: IDraggableLayer? = null

    private val viewState = MutableStateFlow<MapViewState?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            viewState.filterNotNull().collect { notifyViewChanged(it, layers) }
        }
    }

    val isMagnifierVisible: Boolean
        get() = dragLayer?.isMagnifierVisible == true

    fun setOnGeoJsonFeatureClickListener(listener: OnGeoJsonFeatureClickListener?) {
        onGeoJsonFeatureClickListener = listener
        layers.filter { it is GeoJsonLayer<*> }.forEach {
            (it as GeoJsonLayer<*>).setOnFeatureClickListener(listener)
        }
    }


    fun invalidate() {
        layers.forEach {
            try {
                it.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: ERROR HANDLING
            }
        }
    }

    fun setLayers(layers: List<ILayer>) {
        this.layers.filterIsInstance<IAsyncLayer>()
            .forEach { it.setHasUpdateListener(null) }

        this.layers = layers.toList()
        this.layers.filterIsInstance<IAsyncLayer>()
            .forEach { it.setHasUpdateListener { invalidateView() } }
        this.layers.filter { it is GeoJsonLayer<*> }
            .forEach {
                (it as GeoJsonLayer<*>).setOnFeatureClickListener(
                    onGeoJsonFeatureClickListener
                )
            }

        // Layers added after the view last changed still need to load for it
        viewState.value?.let {
            viewState.value = MapViewState(it.context, it.viewBounds, it.bounds, it.projection)
        }
        invalidateView()
    }

    fun getLayers(): List<ILayer> {
        return layers.toList()
    }

    fun start() {
        layers.forEach {
            try {
                it.start()
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: ERROR HANDLING
            }
        }
        invalidateView()
    }

    fun stop() {
        layers.forEach {
            try {
                it.stop()
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: ERROR HANDLING
            }
        }
    }


    fun drawOverlay(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        layers.forEach {
            try {
                drawer.withLayerOpacity(it.opacity) {
                    it.drawOverlay(context, drawer, map)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: ERROR HANDLING
            }
        }
    }

    fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        publishViewState(context, drawer, map)
        layers.forEach {
            try {
                drawer.withLayerOpacity(it.opacity) {
                    it.draw(context, drawer, map)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: ERROR HANDLING
            }
        }
    }

    private fun publishViewState(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        val projection = map.mapProjection
        val bounds = map.mapBounds
        val current = viewState.value
        if (current != null && current.projection === projection && current.bounds == bounds) {
            return
        }
        viewState.value = MapViewState(context, drawer.getBounds(45f), bounds, projection)
    }

    private fun notifyViewChanged(state: MapViewState, layers: List<ILayer>) {
        layers.filterIsInstance<IAsyncLayer>().forEach {
            try {
                it.onViewChanged(state)
            } catch (e: Exception) {
                getAppService<Logger>().error(TAG, "Unable to update layer ${it.layerId}", e)
            }
        }
    }

    fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate) {
        for (layer in layers.reversed()) {
            try {

                val handled = layer.onClick(drawer, map, pixel)
                if (handled) {
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: ERROR HANDLING
            }
        }
    }

    fun onLongPress(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return layers.reversed().any { it.onLongPress(drawer, map, pixel) }
    }

    fun onDragStart(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        dragLayer = layers.reversed()
            .filterIsInstance<IDraggableLayer>()
            .firstOrNull { it.onDragStart(drawer, map, pixel) }
        return dragLayer != null
    }

    fun onDrag(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate) {
        dragLayer?.onDrag(drawer, map, pixel)
    }

    fun onDragEnd(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate) {
        dragLayer?.onDragEnd(drawer, map, pixel)
        dragLayer = null
    }

    fun setTime(time: Instant?) {
        layers.forEach {
            if (it.isTimeDependent) {
                it.setTime(time)
            }
        }
    }

    companion object {
        private const val TAG = "MapViewLayerManager"
    }
}
