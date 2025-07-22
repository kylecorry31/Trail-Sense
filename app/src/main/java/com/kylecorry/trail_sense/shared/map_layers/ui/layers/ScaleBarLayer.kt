package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.paths.ui.DistanceScale

// TODO: Allow position to be adjusted
class ScaleBarLayer : ILayer {

    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    var units: DistanceUnits = DistanceUnits.Meters
        set(value) {
            field = value
            invalidate()
        }

    private val formatter = AppServiceRegistry.get<FormatService>()

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        drawer.noFill()
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)

        val scaleSize =
            distanceScale.getScaleDistance(units, drawer.canvas.width / 2f, map.metersPerPixel)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, map.metersPerPixel, scaleBar)
        val start = drawer.canvas.width - drawer.dp(16f) - drawer.pathWidth(scaleBar)
        val y = drawer.canvas.height - drawer.dp(16f)
        drawer.push()
        drawer.translate(start, y)
        drawer.stroke(Color.BLACK)
        drawer.strokeWeight(8f)
        drawer.path(scaleBar)
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)
        drawer.path(scaleBar)
        drawer.pop()

        drawer.textMode(TextMode.Corner)
        drawer.textSize(drawer.sp(12f))
        drawer.strokeWeight(4f)
        drawer.stroke(Color.BLACK)
        drawer.fill(Color.WHITE)
        val scaleText =
            formatter.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        drawer.text(
            scaleText,
            start - drawer.textWidth(scaleText) - drawer.dp(4f),
            y + drawer.textHeight(scaleText) / 2
        )
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