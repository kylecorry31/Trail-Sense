package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.andromeda_temp.withLayerOpacity
import com.kylecorry.sol.math.SolMath

class NavigationLayer : BaseLayer() {

    private var _start: Coordinate? = null
    private var _end: Coordinate? = null
    @ColorInt
    private var _color: Int = Color.WHITE

    private var opacity: Int = 255

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

    fun setPreferences(prefs: NavigationMapLayerPreferences){
        opacity = SolMath.map(
            prefs.opacity.get().toFloat(),
            0f,
            100f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        drawer.withLayerOpacity(opacity) {
            super.draw(drawer, map)
            val scale = map.layerScale
            val p1 = _start?.let { map.toPixel(it) } ?: return@withLayerOpacity
            val p2 = _end?.let { map.toPixel(it) } ?: return@withLayerOpacity
            drawer.noPathEffect()
            drawer.noFill()
            drawer.stroke(_color)
            drawer.strokeWeight(6f / scale)
            drawer.line(p1.x, p1.y, p2.x, p2.y)
        }
    }
}