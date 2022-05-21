package com.kylecorry.trail_sense.navigation.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

interface ILayer {
    fun draw(drawer: ICanvasDrawer, mapper: ICoordinateToPixelStrategy, scale: Float)
    fun invalidate()
}