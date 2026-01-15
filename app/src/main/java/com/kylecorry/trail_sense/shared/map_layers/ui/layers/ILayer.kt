package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath

interface ILayer {

    /**
     * The ID of the layer
     */
    val layerId: String

    /**
     * Set the preferences of the layer
     */
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

    /**
     * Invalidate the layer
     */
    fun invalidate()

    /**
     * Called when the layer is clicked.
     * @return true if the click event was handled by this layer, false otherwise
     */
    fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean

    /**
     * Called when the layer can start running
     */
    fun start() {
        // Do nothing
    }

    /**
     * Called when the layer should stop running and clean up
     */
    fun stop() {
        // Do nothing
    }

    val percentOpacity: Float

    val opacity: Int
        get() = SolMath.map(
            percentOpacity,
            0f,
            1f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
}