package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.interpolation.Interpolation

import java.time.Instant

interface ILayer {

    val layerId: String

    val isTimeDependent: Boolean
        get() = false

    val isLoaded: Boolean
        get() = true

    fun setTime(time: Instant?) {
    }

    fun setPreferences(preferences: Bundle)

    /**
     * Draw the layer on the map.
     * Transforms are already applied to the canvas.
     */
    fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView)

    /**
     * Draw the overlay on the map.
     * This is drawn on top of the map and is not transformed.
     */
    fun drawOverlay(context: Context, drawer: ICanvasDrawer, map: IMapView)

    fun invalidate()

    /**
     * Called when the layer is clicked.
     * @return true if the click event was handled by this layer, false otherwise
     */
    fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean

    /**
     * Called when the layer is long pressed.
     * @return true if the long press was handled by this layer, false otherwise
     */
    fun onLongPress(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    fun start() {
    }

    fun stop() {
    }

    /**
     * Filter to just the provided feature ID. Pass null to clear the filter.
     */
    fun setFeatureFilter(id: String?) {
    }

    val percentOpacity: Float

    val opacity: Int
        get() = Interpolation.map(
            percentOpacity,
            0f,
            1f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
}
