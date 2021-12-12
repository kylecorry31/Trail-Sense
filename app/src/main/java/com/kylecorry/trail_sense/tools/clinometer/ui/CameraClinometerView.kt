package com.kylecorry.trail_sense.tools.clinometer.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath.map
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.absoluteValue
import kotlin.math.min

class CameraClinometerView : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
    }

    var inclination = 0f
        set(value) {
            field = value
            invalidate()
        }

    var startInclination: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    private val formatter = FormatService(context)
    private val tickInterval = 10
    private var tickLength = 1f
    private val labelInterval = 30
    private var dialColor = Color.BLACK


    override fun setup() {
        dialColor = Resources.color(context, R.color.colorSecondary)
        tickLength = dp(4f)
        textSize(sp(10f))
    }

    override fun draw() {
        drawBackground()
        drawTicks()
        drawNeedle()
    }

    private fun drawBackground(){
        background(dialColor)
        val alpha = 150

        noStroke()
        val w = tickLength

        // High
        fill(AppColor.Red.color)
        opacity(alpha)
        rect(0f, getY(45f), w, (getY(30f) - getY(45f)))
        rect(0f, getY(-30f), w, (getY(-45f) - getY(-30f)))

        // Moderate
        fill(AppColor.Yellow.color)
        opacity(alpha)
        rect(0f, getY(60f), w, (getY(45f) - getY(60f)))
        rect(0f, getY(-45f), w, (getY(-60f) - getY(-45f)))

        // Low
        fill(AppColor.Green.color)
        opacity(alpha)
        rect(0f, getY(90f), w, (getY(60f) - getY(90f)))
        rect(0f, getY(-60f), w, (getY(-90f) - getY(-60f)))
        rect(0f, getY(30f), w, (getY(-30f) - getY(30f)))

        opacity(255)
    }

    private fun drawTicks() {
        strokeWeight(dp(2f))
        for (i in -90..90 step tickInterval) {
            stroke(Color.WHITE)
            val y = getY(i.toFloat())

            line(0f, y, tickLength, y)
            if (i % labelInterval == 0) {
                noStroke()
                fill(Color.WHITE)
                val degrees = i.absoluteValue
                val degreeText = formatter.formatDegrees(degrees.toFloat())
                textMode(TextMode.Center)
                val offset = textWidth(degreeText)
                val x = tickLength + offset
                text(degreeText, x, y)
            }
        }
    }

    private fun drawNeedle() {
        val start = startInclination?.let { getY(it) }
        val current = getY(inclination)

        when {
            start != null -> {
                fill(Color.WHITE)
                opacity(100)
                noStroke()
                rect(
                    0f,
                    min(start, current),
                    width.toFloat(),
                    (start - current).absoluteValue
                )
                opacity(255)
            }
            else -> {
                stroke(Color.WHITE)
                strokeWeight(dp(4f))
                line(0f, current, width.toFloat(), current)
            }
        }
    }

    private fun getY(inclination: Float): Float {
        val padding = 20f//textHeight("9")
        val h = height - 2 * padding
        return h - (map(inclination, -90f, 90f, 0f, 1f) * h) + padding
    }

}