package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.tools.augmented_reality.position.ARPositionStrategy

interface ARMarker: ARPositionStrategy {
    fun draw(view: AugmentedRealityView, drawer: ICanvasDrawer, area: PixelCircle)
    fun onFocused(): Boolean
    fun onClick(): Boolean
}