package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.scales.IColorScale

class ColorScaleView(context: Context, attrs: AttributeSet? = null) : CanvasView(context, attrs) {

    var colorScale: IColorScale? = null
        set(value) {
            field = value
            invalidate()
        }

    var labels: Map<Float, String> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    init {
        runEveryCycle = false
    }

    @ColorInt
    private var textColor: Int = Color.BLACK

    override fun setup() {
        textColor = Resources.androidTextColorSecondary(context)
    }

    override fun draw() {
        clear()
        val colorScale = colorScale ?: return

        textSize(sp(12f))
        val labelPadding = dp(2.5f)
        val labelHeight = (labels.maxOfOrNull { textHeight(it.value) } ?: 0f) + labelPadding * 2

        noFill()
        strokeWeight(1f)
        val scaleHeight = height - labelHeight

        for (i in 0..width) {
            val pct = i / width.toFloat()
            val color = colorScale.getColor(pct)
            stroke(color)
            line(i.toFloat(), 0f, i.toFloat(), scaleHeight)
        }

        textMode(TextMode.Center)

        noStroke()
        fill(textColor)
        for (label in labels){
            text(label.value, width * label.key, height.toFloat() - textHeight(label.value) / 2 - labelPadding)
        }


    }
}