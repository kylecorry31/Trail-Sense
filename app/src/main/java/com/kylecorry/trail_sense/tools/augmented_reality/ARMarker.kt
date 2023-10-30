package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.canvas.PixelCircle

interface ARMarker {
    fun draw(drawer: ICanvasDrawer, anchor: PixelCircle)
    fun getAngularDiameter(view: AugmentedRealityView): Float
    fun getHorizonCoordinate(view: AugmentedRealityView): AugmentedRealityView.HorizonCoordinate
    fun onFocused(): Boolean
    fun onClick(): Boolean
}