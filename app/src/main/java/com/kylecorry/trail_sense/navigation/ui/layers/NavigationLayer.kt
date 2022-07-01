package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate

class NavigationLayer : BaseLayer() {

    private var _start: Coordinate? = null
    private var _end: Coordinate? = null
    @ColorInt private var _color: Int = Color.WHITE

    fun setStart(location: Coordinate?) {
        _start = location
        invalidate()
    }

    fun setEnd(location: Coordinate?) {
        _end = location
        invalidate()
    }

    fun setColor(@ColorInt color: Int){
        _color = color
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        super.draw(drawer, map)
        val scale = map.layerScale
        val p1 = _start?.let { map.toPixel(it) } ?: return
        val p2 = _end?.let { map.toPixel(it) } ?: return
        drawer.noPathEffect()
        drawer.noFill()
        drawer.stroke(_color)
        drawer.strokeWeight(6f / scale)
        drawer.line(p1.x, p1.y, p2.x, p2.y)
    }
}