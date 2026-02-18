package com.kylecorry.trail_sense.tools.clinometer.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.canvas.Dial
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.min

class ClinometerView : CanvasView, IClinometerView {

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

    override var angle = 0f
        set(value) {
            field = value + 90f
            invalidate()
        }

    override var startAngle: Float? = null
        set(value) {
            field = if (value == null) null else value + 90
            invalidate()
        }

    private val formatter = FormatService.getInstance(context)
    private var dialColor = Color.BLACK
    private val tickInterval = 10
    private var tickLength = 1f
    private val needlePercent = 0.75f
    private val dividerPercent = 0.8f
    private val avalancheIndicatorPercent = 0.97f
    private val tickRadiusPercent = 0.9f
    private val tickLengthPercent = 0.03f
    private val labelInterval = 30
    private var radius = 1f

    private lateinit var tickPath: Path

    private val avalancheRiskClipPath = Path()


    override fun setup() {
        dialColor = Resources.color(context, R.color.colorSecondary)
        textSize(sp(10f))
        radius = min(width.toFloat(), height.toFloat()) / 2
        tickLength = radius * tickLengthPercent

        avalancheRiskClipPath.addCircle(
            width / 2f,
            height / 2f,
            radius * avalancheIndicatorPercent,
            Path.Direction.CW
        )
        tickPath =
            Dial.ticks(
                PixelCoordinate(width / 2f, height / 2f),
                radius * tickRadiusPercent,
                tickLength,
                tickInterval
            )
    }

    override fun draw() {
        push()
        drawBackground()
        drawTicks()
        drawNeedle(angle)

        drawLabels()

        push()
        rotate(180f)
        drawLabels()
        pop()

        pop()
    }

    private fun drawAvalancheZone(start: Float, stop: Float, color: Int) {
        val x = width / 2f - radius
        val y = height / 2f - radius
        val d = radius * 2
        noStroke()
        fill(color)
        opacity(150)
        arc(x, y, d, d, start, stop)
    }

    private fun drawBackground() {
        fill(dialColor)
        noStroke()
        circle(width / 2f, height / 2f, radius * 2)

        fill(Color.WHITE)
        opacity(10)
        strokeWeight(3f)
        circle(width / 2f, height / 2f, radius * 2 * dividerPercent)

        push()

        clipInverse(avalancheRiskClipPath)

        // High
        drawAvalancheZone(30f, 45f, AppColor.Red.color)
        drawAvalancheZone(-30f, -45f, AppColor.Red.color)
        drawAvalancheZone(210f, 225f, AppColor.Red.color)
        drawAvalancheZone(-210f, -225f, AppColor.Red.color)

        // Moderate
        drawAvalancheZone(45f, 60f, AppColor.Yellow.color)
        drawAvalancheZone(-45f, -60f, AppColor.Yellow.color)
        drawAvalancheZone(225f, 240f, AppColor.Yellow.color)
        drawAvalancheZone(-225f, -240f, AppColor.Yellow.color)

        // Low
        drawAvalancheZone(-30f, 30f, AppColor.Green.color)
        drawAvalancheZone(-60f, -90f, AppColor.Green.color)
        drawAvalancheZone(60f, 90f, AppColor.Green.color)
        drawAvalancheZone(-210f, -150f, AppColor.Green.color)
        drawAvalancheZone(-240f, -270f, AppColor.Green.color)
        drawAvalancheZone(240f, 270f, AppColor.Green.color)

        opacity(255)

        pop()

    }

    private fun drawTicks() {
        strokeWeight(dp(2f))
        stroke(Color.WHITE)
        opacity(255)
        noFill()
        path(tickPath)
    }

    private fun drawLabels() {
        strokeWeight(dp(2f))
        stroke(dialColor)
        fill(Color.WHITE)

        for (i in 0..180 step labelInterval) {
            push()
            rotate(i.toFloat())
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
            val y = height / 2f - radius * dividerPercent + tickLength + offset
            rotate(180f, x, y)
            text(degreeText, x, y)
            pop()
            pop()
        }
    }

    private fun drawNeedle(angle: Float) {

        startAngle?.let {
            val x = width / 2f - radius
            val y = height / 2f - radius
            fill(Color.WHITE)
            noStroke()
            opacity(127)

            val delta = deltaAngle(it, angle)

            val start = it - 90
            val end = start + delta

            arc(x, y, radius * 2, radius * 2, start, end)
            opacity(255)
        }

        val overhang = radius * 0.1f

        stroke(Color.WHITE)
        strokeWeight(dp(4f))
        push()
        rotate(angle)
        line(width / 2f, height / 2f + overhang, width / 2f, height / 2f - radius * needlePercent)
        pop()

        fill(Color.WHITE)
        stroke(dialColor)
        strokeWeight(dp(1f))
        circle(width / 2f, height / 2f, dp(12f))
    }

}