package com.kylecorry.trail_sense.tools.clinometer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.min

class ClinometerView : CanvasView {

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

    var locked = false
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
    private var lockImage: Bitmap? = null
    private var radius = 1f

    private val avalancheRiskClipPath = Path()


    override fun setup() {
        dialColor = Resources.color(context, R.color.colorSecondary)
        tickLength = dp(4f)
        lockImage = loadImage(R.drawable.lock, dp(24f).toInt(), dp(24f).toInt())
        textSize(sp(10f))
        radius = min(width.toFloat(), height.toFloat()) / 2
        avalancheRiskClipPath.addCircle(
            width / 2f,
            height / 2f,
            radius - tickLength,
            Path.Direction.CW
        )
    }

    override fun draw() {
        push()
        val onLeft = angle in 180.0..360.0
        val realAngle = if (onLeft) {
            angle - 180
        } else {
            angle
        }

        if (onLeft) {
            rotate(180f)
        }

        drawBackground()

        if (locked) {
            drawLock()
        }

        drawTicks()
        drawNeedle(realAngle)
        pop()
    }

    private fun drawBackground() {
        fill(dialColor)
        noStroke()
        circle(width / 2f, height / 2f, radius * 2)

        val x = width / 2f - radius
        val y = height / 2f - radius
        val d = radius * 2

        val alpha = 150

        push()

        clipInverse(avalancheRiskClipPath)

        // High
        fill(AppColor.Red.color)
        opacity(alpha)
        arc(x, y, d, d, 30f, 45f)
        arc(x, y, d, d, -30f, -45f)

        // Moderate
        fill(AppColor.Yellow.color)
        opacity(alpha)
        arc(x, y, d, d, 45f, 60f)
        arc(x, y, d, d, -45f, -60f)

        fill(AppColor.Green.color)
        opacity(alpha)
        arc(x, y, d, d, -30f, 30f)
        arc(x, y, d, d, -60f, -90f)
        arc(x, y, d, d, 60f, 90f)

        opacity(255)

        pop()

    }

    private fun drawTicks() {
        strokeWeight(dp(2f))

        for (i in 0..180 step tickInterval) {
            push()
            rotate(i.toFloat())
            stroke(Color.WHITE)
            line(width / 2f, height / 2f - radius, width / 2f, height / 2f - radius + tickLength)

            if (i % labelInterval == 0) {
                noStroke()
                fill(Color.WHITE)
                val degrees = if (i <= 90) {
                    90 - i
                } else {
                    i - 90
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
    }

    private fun drawNeedle(angle: Float) {
        stroke(Color.WHITE)
        strokeWeight(dp(4f))
        push()
        rotate(angle)
        line(width / 2f, height / 2f, width / 2f, height / 2f - radius * needlePercent)
        pop()

        fill(Color.WHITE)
        noStroke()
        circle(width / 2f, height / 2f, dp(12f))
    }

    private fun drawLock() {
        push()
        rotate(-90f, width / 2f - radius / 2, height / 2f)
        lockImage?.let {
            imageMode(ImageMode.Center)
            image(it, width / 2f - radius / 2, height / 2f)
        }
        pop()
    }

    fun finalize() {
        lockImage?.recycle()
    }


}