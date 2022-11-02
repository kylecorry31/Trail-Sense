package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.time.Time.hoursUntil
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt

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
    private var _gridThickness = 2f
    private var _yLabelCount = 3
    private var _yGridLines = true

    // TODO: Use granularity for default label formatter
    private var _yLabelFormatter: (Float) -> String = { it.roundToInt().toString() }

    init {
        runEveryCycle = false
    }

    override fun setup() {
        _labelSize = sp(10f)
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

        val margin = dp(4f)

        var chartXMin = margin
        val chartXMax = width.toFloat() - margin

        val chartYMin = margin
        val chartYMax = height.toFloat() - margin

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
            val label = _yLabelFormatter(value)
            val yPos = yMap(value)
            yLabels.add(label to yPos)
            yLabelSize = max(yLabelSize, textWidth(label))
        }

        chartXMin += yLabelSize + if (yLabelSize > 0f) dp(4f) else 0f

        for (label in yLabels) {
            textAlign(TextAlign.Right)
            val x = yLabelSize
            val y = label.second + textHeight(label.first) / 2f
            text(label.first, x, y)
        }

        // Y grid lines
        if (_yGridLines) {
            noFill()
            stroke(_gridColor)
            strokeWeight(_gridThickness)
            for (label in yLabels) {
                line(chartXMin, label.second, chartXMax, label.second)
            }
        }

        // TODO: X axis

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

    fun setYLabelCount(count: Int){
        _yLabelCount = count
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

interface ChartData {
    val data: List<Vector2>

    fun draw(drawer: ICanvasDrawer, xMap: (Float) -> Float, yMap: (Float) -> Float)
}

// TODO: Handle on click
class LineChartData(
    override val data: List<Vector2>,
    @ColorInt val color: Int,
    val thickness: Float = 6f
) : ChartData {
    val path = Path()

    override fun draw(drawer: ICanvasDrawer, xMap: (Float) -> Float, yMap: (Float) -> Float) {
        // TODO: Scale rather than recompute
        path.rewind()
        for (i in 1 until data.size) {
            if (i == 1) {
                val start = data[0]
                path.moveTo(xMap(start.x), yMap(start.y))
            }

            val next = data[i]
            path.lineTo(xMap(next.x), yMap(next.y))
        }

        drawer.noFill()
        drawer.strokeWeight(thickness)
        drawer.stroke(color)
        drawer.path(path)
    }
}

class AreaChartData(
    val upper: List<Vector2>,
    val lower: List<Vector2>,
    @ColorInt val color: Int
) : ChartData {

    override val data: List<Vector2> = upper + lower

    val path = Path()

    override fun draw(drawer: ICanvasDrawer, xMap: (Float) -> Float, yMap: (Float) -> Float) {
        // TODO: Scale rather than recompute
        path.rewind()
        // Add upper to path
        for (i in 1 until upper.size) {
            if (i == 1) {
                val start = upper[0]
                path.moveTo(xMap(start.x), yMap(start.y))
            }

            val next = upper[i]
            path.lineTo(xMap(next.x), yMap(next.y))
        }

        // Add lower to path
        for (i in (0..lower.lastIndex).reversed()) {
            val next = lower[i]
            path.lineTo(xMap(next.x), yMap(next.y))
        }

        path.close()

        drawer.fill(color)
        drawer.noStroke()
        drawer.path(path)
    }
}