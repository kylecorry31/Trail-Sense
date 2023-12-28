package com.kylecorry.trail_sense.tools.level.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.trail_sense.shared.CustomUiUtils.getColorOnPrimary
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.math.abs
import kotlin.math.min

class BubbleLevel(context: Context?, attrs: AttributeSet? = null) : CanvasView(context, attrs) {

    private var bubbleColor: Int = Color.BLACK
    private var bubbleRadius: Float = 0f
    private var backgroundColor: Int = Color.WHITE
    private var textColor: Int = Color.BLACK
    private var textSize: Float = 0f
    private var lineColor: Int = Color.BLACK
    private var lineThickness: Float = 0f
    private var bubblePadding: Float = 0f
    private var barRadius: Float = 0f
    private var barGap: Float = 0f

    var xAngle: Float = 0f
    var yAngle: Float = 0f

    private val formatter = FormatService.getInstance(this.context)


    override fun setup() {
        bubbleColor = Resources.getPrimaryColor(context)
        bubbleRadius = dp(16f)
        bubblePadding = dp(8f)
        backgroundColor =
            Resources.getAndroidColorAttr(context, android.R.attr.colorBackgroundFloating)
        textColor = Resources.getColorOnPrimary(context)
        textSize = sp(14f)
        lineColor = Resources.androidTextColorPrimary(context).withAlpha(127)
        lineThickness = dp(2f)
        barRadius = dp(8f)
        barGap = dp(16f)
    }

    override fun draw() {
        val barThickness = bubbleRadius * 2 + bubblePadding * 2
        val barLength = min(width - barThickness - barGap, height - barThickness - barGap)

        val topOffset = (height - barLength) / 2f

        val xBarTop = topOffset
        val xBarLeft = 0f
        val yBarLeft = barLength + barGap
        val yBarTop = barThickness + barGap + topOffset

        // Top bar
        noStroke()
        fill(backgroundColor)
        rect(xBarLeft, xBarTop, barLength, barThickness, barRadius)

        // Right bar
        rect(
            yBarLeft,
            yBarTop,
            barThickness,
            barLength,
            barRadius
        )

        // Center circle
        circle(
            xBarLeft + barLength / 2f,
            yBarTop + barLength / 2f,
            barLength
        )

        // Center crosshair
        stroke(lineColor)
        noFill()
        strokeWeight(lineThickness)
        line(
            xBarLeft + barLength / 2f,
            yBarTop,
            xBarLeft + barLength / 2f,
            yBarTop + barLength
        )
        line(
            xBarLeft,
            yBarTop + barLength / 2f,
            xBarLeft + barLength,
            yBarTop + barLength / 2f
        )

        // Top line
        line(
            xBarLeft + barLength / 2f,
            xBarTop,
            xBarLeft + barLength / 2f,
            xBarTop + barThickness
        )

        // Right line
        line(
            yBarLeft,
            yBarTop + barLength / 2f,
            yBarLeft + barThickness,
            yBarTop + barLength / 2f
        )

        // Top bubble
        fill(bubbleColor)
        noStroke()

        val xPercent = (xAngle + 90) / 180f
        circle(
            xBarLeft + xPercent * barLength,
            xBarTop + barThickness / 2,
            bubbleRadius * 2
        )

        // Right bubble
        val yPercent = (yAngle + 90) / 180f
        circle(
            yBarLeft + barThickness / 2,
            yBarTop + yPercent * barLength,
            bubbleRadius * 2
        )

        // Center bubble
        circle(
            xBarLeft + xPercent * barLength,
            yBarTop + yPercent * barLength,
            bubbleRadius * 2
        )

        // Text
        val xText = formatter.formatDegrees(abs(xAngle))
        val yText = formatter.formatDegrees(abs(yAngle))
        fill(textColor)
        textSize(textSize)
        textMode(TextMode.Center)
        text(xText, xBarLeft + barLength * xPercent, xBarTop + barThickness / 2f)
        text(yText, yBarLeft + barThickness / 2f, yBarTop + barLength * yPercent)
    }

}