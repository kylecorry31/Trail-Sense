package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.graphics.Bitmap
import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel

class CellTowerMapLayer(private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    IAsyncLayer, BaseLayer() {

    private var bitmap: Bitmap? = null

    private val minZoomLevel = 10
    private var updateListener: (() -> Unit)? = null

    init {
        taskRunner.addTask { bounds, metersPerPixel ->
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            )

            if (zoomLevel < minZoomLevel) {
                return@addTask
            }

            val towers = CellTowerModel.getTowers(bounds)
            clearMarkers()
            towers.forEach {
                bitmap?.let { bitmap ->
                    addMarker(
                        BitmapMapMarker(
                            it,
                            bitmap,
                            tint = Color.WHITE
                        )
                    )
                }
            }
            updateListener?.invoke()

        }
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled() || map.metersPerPixel > 75f) {
            return
        }

        if (bitmap == null) {
            val size = drawer.dp(12f).toInt()
            bitmap = drawer.loadImage(R.drawable.cell_tower, size, size)
        }

        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel)
        super.draw(drawer, map)
    }

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    protected fun finalize() {
        bitmap?.recycle()
        bitmap = null
    }
}