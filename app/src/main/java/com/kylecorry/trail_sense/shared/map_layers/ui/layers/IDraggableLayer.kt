package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate

/**
 * A layer that can capture a touch to drag something on the map, instead of the map panning.
 */
interface IDraggableLayer : ILayer {

    /**
     * Whether to show a magnified view of the map under the touch while dragging.
     */
    val isMagnifierVisible: Boolean
        get() = false

    /**
     * Called when a touch starts on the map, before any pan or zoom handling.
     * @return true to capture the touch for dragging, which suppresses map panning and zooming
     */
    fun onDragStart(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean

    /**
     * Called as a captured touch moves.
     */
    fun onDrag(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate)

    /**
     * Called when a captured touch is released or cancelled.
     */
    fun onDragEnd(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate)
}
