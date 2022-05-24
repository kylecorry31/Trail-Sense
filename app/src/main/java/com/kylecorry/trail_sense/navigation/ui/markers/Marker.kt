package com.kylecorry.trail_sense.navigation.ui.markers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

interface Marker {
    val location: Coordinate

    fun draw(drawer: ICanvasDrawer, anchor: PixelCoordinate, scale: Float, rotation: Float)
}