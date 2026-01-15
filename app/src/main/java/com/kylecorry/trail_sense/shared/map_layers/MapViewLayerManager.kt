package com.kylecorry.trail_sense.shared.map_layers

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.withLayerOpacity
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

class MapViewLayerManager(private val invalidateView: () -> Unit) {

    private var layers = listOf<ILayer>()


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

    fun addLayer(layer: ILayer) {
        layers = layers + layer
    }

    fun removeLayer(layer: ILayer) {
        if (layer is IAsyncLayer) {
            layer.setHasUpdateListener(null)
        }
        layers = layers - layer
    }

    fun setLayers(layers: List<ILayer>) {
        this.layers.filterIsInstance<IAsyncLayer>()
            .forEach { it.setHasUpdateListener(null) }

        this.layers = layers.toList()
        this.layers.filterIsInstance<IAsyncLayer>()
            .forEach { it.setHasUpdateListener { invalidateView() } }

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

}