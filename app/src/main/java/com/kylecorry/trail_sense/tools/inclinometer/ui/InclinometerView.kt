package com.kylecorry.trail_sense.tools.inclinometer.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.min

class InclinometerView : CanvasView {

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

    var angle = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val formatter = FormatService(context)
    private var dialColor = Color.BLACK
    private val tickInterval = 10
    private var tickLength = 1f
    private val needlePercent = 0.8f
    private val labelInterval = 30


    override fun setup() {
        dialColor = Resources.color(context, R.color.colorSecondary)
        tickLength = dp(4f)
        textSize(sp(10f))
    }

    override fun draw() {
        val radius = min(width.toFloat(), height.toFloat()) / 2

        // Background
        fill(dialColor)
        noStroke()
        circle(width / 2f, height / 2f, radius * 2)

        // Ticks
        strokeWeight(dp(2f))

        val onLeft = angle in 180.0..360.0

        val tickRange = if (onLeft) {
            180..360
        } else {
            0..180
        }

        // TODO: Draw avalanche risk zones

        for (i in tickRange step tickInterval) {
            push()
            rotate(i.toFloat())
            stroke(Color.WHITE)
            line(width / 2f, height / 2f - radius, width / 2f, height / 2f - radius + tickLength)

            if (i % labelInterval == 0) {
                noStroke()
                fill(Color.WHITE)
                val degrees = if (onLeft) {
                    if (i <= 270) {
                        270 - i
                    } else {
                        i - 270
                    }
                } else {
                    if (i <= 90) {
                        90 - i
                    } else {
                        i - 90
                    }
                }

                val degreeText = formatter.formatDegrees(degrees.toFloat())
                textMode(TextMode.Center)
                val offset = textHeight(degreeText)
                push()
                val x = width / 2f
                val y = height / 2f - radius + tickLength + offset
                rotate(180f, x, y)
                text(degreeText, x, y)
                pop()
            }

            pop()
        }

        // Needle
        stroke(Color.WHITE)
        push()
        rotate(angle)
        line(width / 2f, height / 2f, width / 2f, height / 2f - radius * needlePercent)
        pop()

        fill(Color.WHITE)
        noStroke()
        circle(width / 2f, height / 2f, dp(12f))
    }


}