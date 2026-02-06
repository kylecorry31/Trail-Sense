package com.kylecorry.trail_sense.tools.navigation.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.Dial

class CompassDial(
    private val center: PixelCoordinate,
    private val radius: Float,
    @ColorInt private val backgroundColor: Int,
    @ColorInt private val tickColor: Int,
    @ColorInt private val cardinalTickColor: Int = tickColor,
    private val tickRadius: Float = 0.9f * radius,
    private val tickLength: Float = 0.03f * radius,
    private val hideTrueCardinalTicks: Boolean = false
) {

    private val tickThicknessDp = 1.5f
    private val ticks = Dial.ticks(
        center,
        tickRadius,
        tickLength,
        15
    )
    private val cardinalTicks = Dial.ticks(
        center,
        tickRadius,
        tickLength,
        45
    )

    private val trueCardinalTicks = Dial.ticks(
        center,
        tickRadius,
        tickLength,
        90
    )

    private var clipPath: Path? = null

    fun draw(drawer: ICanvasDrawer, drawTicks: Boolean = true, drawBackground: Boolean = true) {
        val trueCardinalTickSize = tickThicknessDp * 2f

        drawer.opacity(255)
        drawer.noStroke()
        drawer.fill(backgroundColor)
        if (drawBackground) {
            drawer.circle(center.x, center.y, radius * 2)
        }

        if (!drawTicks) return

        drawer.push()
        if (hideTrueCardinalTicks) {
            if (clipPath == null) {
                clipPath = Path()
                val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = drawer.dp(trueCardinalTickSize + 1)
                    strokeCap = Paint.Cap.SQUARE
                }
                clipPaint.getFillPath(trueCardinalTicks, clipPath)
            }
            drawer.clipInverse(clipPath!!)
        }

        // Outlines
        drawer.strokeCap(StrokeCap.Square)
        drawer.strokeWeight(drawer.dp(tickThicknessDp + 1))
        drawer.noFill()
        drawer.stroke(Color.BLACK)
        drawer.path(ticks)
        drawer.path(cardinalTicks)
        if (!hideTrueCardinalTicks) {
            drawer.strokeWeight(drawer.dp(trueCardinalTickSize + 1))
            drawer.path(trueCardinalTicks)
        }


        // Normal ticks
        drawer.strokeWeight(drawer.dp(tickThicknessDp))
        drawer.stroke(tickColor)
        drawer.path(ticks)

        // Cardinal ticks
        drawer.stroke(cardinalTickColor)
        drawer.path(cardinalTicks)
        if (!hideTrueCardinalTicks) {
            drawer.strokeWeight(drawer.dp(trueCardinalTickSize))
            drawer.path(trueCardinalTicks)
        }

        drawer.strokeCap(StrokeCap.Round)

        drawer.pop()
    }

}