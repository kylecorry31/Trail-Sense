package com.kylecorry.trail_sense.navigation.ui.markers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Bearing

interface CompassMarker {
    val bearing: Bearing
    val size: Float

    fun draw(drawer: ICanvasDrawer, anchor: PixelCoordinate, scale: Float, rotation: Float)
}