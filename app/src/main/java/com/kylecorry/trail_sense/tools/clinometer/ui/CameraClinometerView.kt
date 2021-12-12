package com.kylecorry.trail_sense.tools.clinometer.ui

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.sol.math.SolMath.deltaAngle
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

    var angle = 0f
        set(value) {
            field = value + 90f
            invalidate()
        }

    var imageAngleCalculator: ImageAngleCalculator? = null
        set(value) {
            field = value
            invalidate()
        }

    var startAngle: Float? = null
        set(value) {
            field = if (value == null) null else value + 90
            invalidate()
        }

    var endAngle: Float? = null
        set(value) {
            field = if (value == null) null else value + 90
            invalidate()
        }

    private val formatter = FormatService(context)
    private val tickInterval = 10
    private var tickLength = 1f
    private val labelInterval = 30
    private var lineColor = AppColor.Orange.color


    override fun setup() {
        tickLength = dp(4f)
        textSize(sp(10f))
    }

    override fun draw() {
        drawTicks()
        drawNeedle()
    }

    private fun drawTicks() {
        // TODO: Draw the ticks
    }

    private fun drawNeedle() {
        val start = startAngle?.let { getPercent(it) }
        val end = endAngle?.let { getPercent(it) }

        when {
            start != null -> {
                fill(lineColor)
                opacity(100)
                noStroke()
                if (end != null) {
                    rect(0f, min(start, end), width.toFloat(), (start - end).absoluteValue)
                } else {
                    rect(
                        0f,
                        min(start, height / 2f),
                        width.toFloat(),
                        (start - height / 2f).absoluteValue
                    )
                }
                opacity(255)
            }
            end != null -> {
                stroke(lineColor)
                strokeWeight(dp(4f))
                line(0f, end, width.toFloat(), end)
            }
            else -> {
                stroke(lineColor)
                strokeWeight(dp(4f))
                line(0f, height / 2f, width.toFloat(), height / 2f)
            }
        }
    }

    private fun getPercent(a: Float): Float? {
        val delta = deltaAngle(angle, a)
        val calculator = imageAngleCalculator ?: return null
        return (calculator.getImagePercent(0f, delta).y * height).coerceIn(
            0f,
            height.toFloat()
        )
    }

}