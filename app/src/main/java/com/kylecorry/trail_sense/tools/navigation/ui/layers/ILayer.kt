package com.kylecorry.trail_sense.tools.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate

interface ILayer {
    /**
     * Draw the layer on the map.
     * Transforms are already applied to the canvas.
     */
    fun draw(drawer: ICanvasDrawer, map: IMapView)

    /**
     * Draw the overlay on the map.
     * This is drawn on top of the map and is not transformed.
     */
    fun drawOverlay(drawer: ICanvasDrawer, map: IMapView)

    /**
     * Invalidate the layer
     */
    fun invalidate()

    /**
     * Called when the layer is clicked.
     * @return true if the click event was handled by this layer, false otherwise
     */
    fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean
}