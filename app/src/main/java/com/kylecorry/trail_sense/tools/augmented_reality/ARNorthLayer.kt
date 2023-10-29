package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate

class ARNorthLayer(
    @ColorInt private val color: Int = Color.WHITE,
    private val thicknessDp: Float = 1f,
    private val resolutionDegrees: Int = 5
) : ARLayer {

    private val path = Path()

    // TODO: Make this into a generic path layer
    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        path.reset()
        for (i in -90..90 step resolutionDegrees) {
            val pixel = view.toPixel(AugmentedRealityView.HorizonCoordinate(0f, i.toFloat()))
            if (i == -90) {
                path.moveTo(pixel.x, pixel.y)
            } else {
                path.lineTo(pixel.x, pixel.y)
            }
        }

        drawer.noFill()
        drawer.stroke(color)
        drawer.strokeWeight(drawer.dp(thicknessDp))
        drawer.path(path)
        drawer.noStroke()
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return false
    }
}