package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

/**
 * An augmented reality layer
 */
interface ARLayer {
    /**
     * Update the layer's state. Some canvas drawer functions may be unreliable (ex. canvas.width - use view.width instead)
     */
    suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView)

    /**
     * Draw the layer
     */
    fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView)

    /**
     * Invalidate the layer
     */
    fun invalidate()

    /**
     * Called when the layer is clicked.
     * @return true if the click event was handled by this layer, false otherwise
     */
    fun onClick(drawer: ICanvasDrawer, view: AugmentedRealityView, pixel: PixelCoordinate): Boolean

    /**
     * Called when the AR View is looking to determine which layer is in focus
     * @return true if this layer claims focus, false otherwise
     */
    fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean

}