package com.kylecorry.trail_sense.tools.map.map_layers

import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

class BackgroundColorMapLayer : ILayer {

    override val layerId: String = LAYER_ID

    var color: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    override fun setPreferences(preferences: Bundle) {
        // Do nothing
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

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity

    companion object {
        const val LAYER_ID = "background_color"
    }
}