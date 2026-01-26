package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer
import com.kylecorry.trail_sense.tools.paths.ui.DistanceScale

// TODO: Allow position to be adjusted
class ScaleBarLayer : OverlayLayer() {

    override val layerId: String = LAYER_ID

    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    private val prefs = AppServiceRegistry.get<UserPreferences>()

    private val units = prefs.baseDistanceUnits

    private val formatter = AppServiceRegistry.get<FormatService>()

    private val bottomMargin: Float = 32f

    override fun drawOverlay(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        drawer.noFill()
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)

        val scaleSize =
            distanceScale.getScaleDistance(units, drawer.canvas.width / 2f, map.resolutionPixels)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, map.resolutionPixels, scaleBar)
        val start = drawer.canvas.width - drawer.dp(16f) - drawer.pathWidth(scaleBar)
        val y = drawer.canvas.height - drawer.dp(bottomMargin)
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

    companion object {
        const val LAYER_ID = "scale_bar"
    }
}