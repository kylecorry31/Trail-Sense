package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate

interface IDraggableLayer : ILayer {

    val isMagnifierVisible: Boolean
        get() = false

    /**
     * Called when a touch starts on the map, before any pan or zoom handling.
     * @return true to capture the touch for dragging, which suppresses map panning and zooming
     */
    fun onDragStart(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean

    fun onDrag(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate)

    fun onDragEnd(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate)
}
