package com.kylecorry.trail_sense.shared.views.chart

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.time.Time.hoursUntil
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import java.time.Instant
import kotlin.math.max

class Chart : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var _data = emptyList<ChartData>()
    private var _backgroundColor = Color.TRANSPARENT
    private var _labelColor = Color.BLACK
    private var _gridColor = Color.BLACK
    private var _labelSize = 0f
    private var _labelMargin = 0f
    private var _gridThickness = 2f
    private var _margin = 0f

    // Y axis
    private var _yLabelCount = 3
    private var _yGridLines = true
    private var _yLabelFormatter: ChartLabelFormatter = NumberChartLabelFormatter()

    // X axis
    private var _xLabelCount = 3
    private var _xGridLines = true
    private var _xLabelFormatter: ChartLabelFormatter = NumberChartLabelFormatter()

    init {
        runEveryCycle = false
    }

    override fun setup() {
        _labelSize = sp(10f)
        _margin = dp(8f)
        _labelMargin = dp(4f)
        _labelColor = Resources.androidTextColorPrimary(context).withAlpha(150)
        _gridColor = Resources.androidTextColorPrimary(context).withAlpha(50)
    }

    override fun draw() {
        clear()
        background(_backgroundColor)

        // Update min / max
        var xMin = Float.POSITIVE_INFINITY
        var xMax = Float.NEGATIVE_INFINITY
        var yMin = Float.POSITIVE_INFINITY
        var yMax = Float.NEGATIVE_INFINITY

        for (d in _data) {
            for (point in d.data) {
                if (point.x < xMin) {
                    xMin = point.x
                }

                if (point.x > xMax) {
                    xMax = point.x
                }

                if (point.y < yMin) {
                    yMin = point.y
                }

                if (point.y > yMax) {
                    yMax = point.y
                }
            }
        }

        var chartXMin = _margin
        val chartXMax = width.toFloat() - _margin

        val chartYMin = _margin
        var chartYMax = height.toFloat() - _margin

        val xMap: (Float) -> Float = { SolMath.map(it, xMin, xMax, chartXMin, chartXMax) }
        val yMap: (Float) -> Float = { -SolMath.map(it, yMin, yMax, -chartYMax, -chartYMin) }

        // Y axis labels
        textSize(_labelSize)
        fill(_labelColor)
        noStroke()
        val yLabels = mutableListOf<Pair<String, Float>>()
        var yLabelSize = 0f
        for (i in 0 until _yLabelCount) {
            val value = SolMath.lerp(i / (_yLabelCount - 1).toFloat(), yMin, yMax)
            val label = _yLabelFormatter.format(value)
            yLabels.add(label to value)
            yLabelSize = max(yLabelSize, textWidth(label))
        }

        // X axis labels
        val xLabels = mutableListOf<Pair<String, Float>>()
        var xLabelSize = 0f
        for (i in 0 until _xLabelCount) {
            val value = SolMath.lerp(i / (_xLabelCount - 1).toFloat(), xMin, xMax)
            val label = _xLabelFormatter.format(value)
            xLabels.add(label to value)
            xLabelSize = max(xLabelSize, textHeight(label))
        }

        chartXMin += yLabelSize + if (_yLabelCount > 0f) _labelMargin else 0f
        chartYMax -= xLabelSize + if (_xLabelCount > 0f) _labelMargin else 0f

        // Draw y labels
        for (label in yLabels) {
            textAlign(TextAlign.Right)
            val x = yLabelSize
            val y = yMap(label.second) + textHeight(label.first) / 2f
            text(label.first, x, y)
        }

        // Draw x labels
        for (label in xLabels) {
            textAlign(TextAlign.Left)
            val x = xMap(label.second) - textWidth(label.first) / 2f
            val y = height.toFloat() - _margin
            text(label.first, x, y)
        }

        // Y grid lines
        if (_yGridLines) {
            noFill()
            stroke(_gridColor)
            strokeWeight(_gridThickness)
            for (label in yLabels) {
                line(chartXMin, yMap(label.second), chartXMax, yMap(label.second))
            }
        }

        // X grid lines
        if (_xGridLines) {
            noFill()
            stroke(_gridColor)
            strokeWeight(_gridThickness)
            for (label in xLabels) {
                line(xMap(label.second), chartYMin, xMap(label.second), chartYMax)
            }
        }

        // Data
        _data.forEach {
            it.draw(this, xMap, yMap)
        }
    }

    fun plot(data: List<ChartData>) {
        _data = data
        invalidate()
    }

    fun plot(vararg data: ChartData) {
        plot(data.toList())
    }

    fun setYLabelCount(count: Int) {
        _yLabelCount = count
        invalidate()
    }

    fun setXLabelCount(count: Int) {
        _xLabelCount = count
        invalidate()
    }

    companion object {
        fun <T> getDataFromReadings(
            readings: List<Reading<T>>,
            startTime: Instant? = null,
            getY: (T) -> Float
        ): List<Vector2> {
            val first = startTime ?: readings.firstOrNull()?.time ?: return emptyList()
            return readings.map {
                Vector2(first.hoursUntil(it.time), getY(it.value))
            }
        }
    }


}

