package com.kylecorry.trail_sense.tools.ruler.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.roundToInt

class RulerView : CanvasView {

    private val prefs by lazy { UserPreferences(context) }

    private var dpi: Float = 1f
    private var scale: Float = 1f
    private var lineThickness: Float = 2f
    private var highlightLineThickness: Float = 4f
    private var lineColor: Int = Color.BLACK
    private var highlightColor: Int = Color.BLACK
    private var offset: Float = 0f

    private var wholeSize = 0f
    private var halfSize = 0f
    private var quarterSize = 0f
    private var eighthSize = 0f
    private var tenthSize = 0f

    var metric: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var highlight: Distance? = null
        set(value) {
            field = value
            invalidate()
        }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
        setupAfterVisible = true
    }

    override fun setup() {
        dpi = Screen.ydpi(context)
        scale = prefs.navigation.rulerScale
        lineColor = Resources.androidTextColorPrimary(context)
        highlightColor = AppColor.Orange.color
        offset = dp(8f)
        wholeSize = dp(40f)
        halfSize = dp(24f)
        quarterSize = dp(12f)
        eighthSize = dp(6f)
        tenthSize = dp(12f)

        lineThickness = dp(1f)
        highlightLineThickness = dp(2f)

        textSize(sp(12f))
    }

    override fun draw() {
        if (!isVisible) {
            return
        }

        val rulerHeight = getRulerHeight()
        val units = rulerHeight.units

        if (metric) {
            drawLines(
                Distance(0f, units),
                rulerHeight,
                Distance(0.1f, units)
            ) {
                if (approxDivisibleBy(it.distance, 1f)) {
                    wholeSize
                } else if (approxDivisibleBy(it.distance, 0.5f)) {
                    halfSize
                } else {
                    tenthSize
                }
            }
        } else {
            drawLines(
                Distance(0f, units),
                rulerHeight,
                Distance(0.125f, units)
            ) {
                if (it.distance % 1 == 0f) {
                    wholeSize
                } else if (it.distance % 0.5f == 0f) {
                    halfSize
                } else if (it.distance % 0.25f == 0f) {
                    quarterSize
                } else if (it.distance % 0.125f == 0f) {
                    eighthSize
                } else {
                    tenthSize
                }
            }
        }

        drawLabels(
            Distance(0f, units),
            rulerHeight,
            Distance(1f, units),
            wholeSize
        )


        highlight?.let {
            drawLine(it, width.toFloat(), highlightColor, highlightLineThickness)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setOnTouchListener(listener: (Distance) -> Unit) {
        setOnTouchListener { _, event ->
            val distance = getDistance(event.y)
            listener.invoke(distance)
            true
        }
    }

    private fun drawLines(
        start: Distance,
        end: Distance,
        spacing: Distance,
        widthFn: (Distance) -> Float
    ) {
        val convertedSpacing = spacing.convertTo(start.units)
        var position = start
        while (position < end) {
            val lineWidth = widthFn(position)
            drawLine(position, lineWidth, lineColor, lineThickness)
            position = Distance(position.distance + convertedSpacing.distance, position.units)
        }
    }

    private fun drawLabels(
        start: Distance,
        end: Distance,
        spacing: Distance,
        lineWidth: Float,
    ) {
        val convertedSpacing = spacing.convertTo(start.units)
        var position = start
        while (position < end) {
            val label = position.distance.toInt().toString()
            val labelHeight = textHeight(label)
            val y = getPosition(position)
            fill(lineColor)
            noStroke()
            text(label, lineWidth + dp(8f), y + labelHeight / 2)
            position = Distance(position.distance + convertedSpacing.distance, position.units)
        }
    }

    private fun approxDivisibleBy(value: Float, divisor: Float): Boolean {
        val rounded = (value / divisor).roundToInt() * divisor
        return SolMath.isCloseTo(rounded, value, 0.05f)
    }

    private fun drawLine(distance: Distance, lineWidth: Float, lineColor: Int, lineWeight: Float) {
        val y = getPosition(distance)
        stroke(lineColor)
        strokeWeight(lineWeight)
        line(0f, y, lineWidth, y)
    }


    private fun getPosition(distance: Distance): Float {
        val rulerHeight = getRulerHeight()
        val d = distance.convertTo(rulerHeight.units).distance
        return d / rulerHeight.distance * height + offset
    }

    private fun getRulerHeight(): Distance {
        val actualHeight = height - offset
        val heightIn = scale * actualHeight / dpi
        return Distance(
            heightIn,
            DistanceUnits.Inches
        ).convertTo(if (metric) DistanceUnits.Centimeters else DistanceUnits.Inches)
    }

    private fun getDistance(y: Float): Distance {
        val rulerHeight = getRulerHeight()
        val distance = (y - offset) / height * rulerHeight.distance
        return Distance(distance, rulerHeight.units)
    }
}