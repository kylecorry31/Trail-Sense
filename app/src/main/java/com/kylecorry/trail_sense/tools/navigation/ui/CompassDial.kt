package com.kylecorry.trail_sense.tools.navigation.ui

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.math.cos
import kotlin.math.sin

class CompassDial(
    private val center: PixelCoordinate,
    private val tickRadius: Float,
    private val backgroundRadius: Float = tickRadius,
    @ColorInt private val backgroundColor: Int,
    @ColorInt private val tickColor: Int,
    @ColorInt private val cardinalTickColor: Int = tickColor,
    private val hideTrueCardinalTicks: Boolean = false,
    tickLength: Float = 1f,
    cardinalTickLength: Float = tickLength,
    private val bezelThickness: Float = 0f,
    @ColorInt private val bezelColor: Int = backgroundColor
) {

    private val tickThicknessDp = 2f

    private val ticks = tickLines(tickRadius, tickLength, 15)
    private val cardinalTicks = tickLines(tickRadius, cardinalTickLength, 45)
    private val trueCardinalTicks = tickLines(tickRadius, cardinalTickLength, 90)

    private fun tickLines(radius: Float, length: Float, spacing: Int): FloatArray {
        val angles = (0 until 360 step spacing).filterNot { hideTrueCardinalTicks && it % 90 == 0 }
        val lines = FloatArray(angles.size * 4)
        angles.forEachIndexed { i, angle ->
            val dx = cos(Math.toRadians(angle.toDouble())).toFloat()
            val dy = sin(Math.toRadians(angle.toDouble())).toFloat()
            lines[i * 4] = center.x + dx * (radius - length / 2)
            lines[i * 4 + 1] = center.y + dy * (radius - length / 2)
            lines[i * 4 + 2] = center.x + dx * (radius + length / 2)
            lines[i * 4 + 3] = center.y + dy * (radius + length / 2)
        }
        return lines
    }

    fun draw(drawer: ICanvasDrawer, drawTicks: Boolean = true, drawBackground: Boolean = true) {
        val hasBackground = drawBackground && (Color.alpha(backgroundColor) > 0 || bezelThickness > 0f)
        if (!drawTicks && !hasBackground) {
            return
        }

        val trueCardinalTickSize = tickThicknessDp * 1.5f

        drawer.opacity(255)
        drawer.noStroke()
        drawer.fill(backgroundColor)
        if (drawBackground) {
            drawer.circle(center.x, center.y, backgroundRadius * 2)
            if (bezelThickness > 0f) {
                drawer.push()
                drawer.noFill()
                drawer.stroke(bezelColor)
                drawer.strokeWeight(bezelThickness)
                drawer.circle(center.x, center.y, (backgroundRadius + bezelThickness / 2f) * 2)
                drawer.pop()
            }
        }

        if (!drawTicks) return

        drawer.push()
        drawer.strokeCap(StrokeCap.Square)
        drawer.noFill()
        drawer.stroke(Color.BLACK)
        drawer.strokeWeight(drawer.dp(tickThicknessDp + 1))
        drawer.lines(ticks)
        drawer.strokeWeight(drawer.dp(trueCardinalTickSize + 1))
        drawer.lines(cardinalTicks)
        if (!hideTrueCardinalTicks) {
            drawer.lines(trueCardinalTicks)
        }

        drawer.stroke(tickColor)
        drawer.strokeWeight(drawer.dp(tickThicknessDp))
        drawer.lines(ticks)
        drawer.stroke(cardinalTickColor)
        drawer.strokeWeight(drawer.dp(trueCardinalTickSize))
        drawer.lines(cardinalTicks)
        if (!hideTrueCardinalTicks) {
            drawer.lines(trueCardinalTicks)
        }
        drawer.pop()
    }

}
