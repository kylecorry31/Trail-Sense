package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate

interface ILayer {
    fun draw(drawer: ICanvasDrawer, map: IMapView)
    fun invalidate()

    /**
     * Called when the layer is clicked.
     * @return true if the click event was handled by this layer, false otherwise
     */
    fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean
}