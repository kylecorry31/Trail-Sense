package com.kylecorry.trail_sense.tools.navigation.ui.compass.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.tools.navigation.ui.compass.ICompassView

interface ICompassLayer {
    fun draw(drawer: ICanvasDrawer, compass: ICompassView)
    fun invalidate()
}
