package com.kylecorry.trail_sense.shared.art

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate

object Artwork {

    fun shouldShowArtwork(): Boolean {
        return true
    }

    fun shouldDrawShadows(): Boolean {
        return true
    }

    fun shouldDrawIn3D(): Boolean {
        return true
    }


    fun circleHousingPadding(drawer: ICanvasDrawer): Float {
        var padding = 0f
        if (shouldDrawIn3D()) {
            if (shouldDrawShadows()) {
                padding = drawer.dp(10f)
            } else {
                padding = drawer.dp(5f)
            }
            padding += drawer.dp(STROKE_SIZE_DP)
        }
        return padding
    }

    fun drawCircleHousing(drawer: ICanvasDrawer, center: PixelCoordinate, dialDiameter: Float, bezelSize: Float = drawer.dp(14f)) {
        val strokeSize = drawer.dp(STROKE_SIZE_DP)

        if (shouldDrawIn3D()) {
            if (shouldDrawShadows()) {
                // Shadow
                drawer.fill(COLOR_SHADOW)
                drawer.noStroke()
                drawer.circle(
                    center.x,
                    center.y + drawer.dp(10f),
                    dialDiameter + bezelSize * 2
                )
            }

            // Bottom
            drawer.fill(COLOR_METAL_1)
            drawer.strokeWeight(strokeSize)
            drawer.stroke(COLOR_STROKE)
            drawer.circle(center.x, center.y + drawer.dp(5f), dialDiameter + bezelSize * 2)
        }

        // Outer ring
        drawer.fill(COLOR_STROKE)
        drawer.noStroke()
        drawer.circle(center.x, center.y, dialDiameter + bezelSize * 2)

        // Metal
        drawer.fill(COLOR_METAL_2)
        drawer.circle(center.x, center.y, dialDiameter + bezelSize * 2 - strokeSize * 2)

        // Inner ring
        drawer.fill(COLOR_STROKE)
        drawer.noStroke()
        drawer.circle(
            center.x,
            center.y,
            dialDiameter + bezelSize * 2 - strokeSize * 2 - drawer.dp(4f)
        )
    }


    val COLOR_METAL_1 = "#9a9181".toColorInt()
    val COLOR_METAL_2 = "#c2b7a4".toColorInt()
    val COLOR_SHADOW = Color.argb(127, 0, 0, 0)
    const val COLOR_STROKE = Color.BLACK
    const val STROKE_SIZE_DP = 2f

}