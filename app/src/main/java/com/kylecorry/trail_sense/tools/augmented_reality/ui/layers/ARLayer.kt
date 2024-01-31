package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

interface ARLayer {
    // TODO: Use an interface for the view
    fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView)
    fun invalidate()

    /**
     * Called when the layer is clicked.
     * @return true if the click event was handled by this layer, false otherwise
     */
    fun onClick(drawer: ICanvasDrawer, view: AugmentedRealityView, pixel: PixelCoordinate): Boolean

    fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean

}