package com.kylecorry.trail_sense.tools.map.ui

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView

class BackgroundColorMapLayer : ILayer {

    var color: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        drawer.canvas.drawColor(color)
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }
}