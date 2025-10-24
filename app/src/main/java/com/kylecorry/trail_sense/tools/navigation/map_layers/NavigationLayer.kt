package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer

class NavigationLayer : BaseLayer() {

    private val pathLayer = PathLayer()

    private var _start: Coordinate? = null
    private var _end: Coordinate? = null

    @ColorInt
    private var _color: Int = Color.WHITE

    fun setStart(location: Coordinate?) {
        _start = location
        updatePathLayer()
        invalidate()
    }

    fun setEnd(location: Coordinate?) {
        _end = location
        updatePathLayer()
        invalidate()
    }

    fun setColor(@ColorInt color: Int) {
        _color = color
        updatePathLayer()
        invalidate()
    }

    fun setPreferences(prefs: NavigationMapLayerPreferences) {
        setPercentOpacity(prefs.opacity.get() / 100f)
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        super.draw(drawer, map)
        pathLayer.draw(drawer, map)
    }

    private fun updatePathLayer() {
        val start = _start
        val end = _end
        val color = _color

        val path = if (start == null || end == null) {
            null
        } else {
            MappablePath(
                -1, listOf(
                    MappableLocation(-1, start, color, null),
                    MappableLocation(-2, end, color, null)
                ), color, LineStyle.Solid
            )
        }

        pathLayer.setPaths(listOfNotNull(path))
    }
}