package com.kylecorry.trail_sense.shared.views.chart

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.time.Time.hoursUntil
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import com.kylecorry.trail_sense.shared.views.chart.data.ChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.ScatterChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.ChartLabelFormatter
import com.kylecorry.trail_sense.shared.views.chart.label.NumberChartLabelFormatter
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

class Chart : CanvasView, IChart {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var _layers = emptyList<ChartLayer>()
    private var _backgroundColor = Color.TRANSPARENT
    private var _labelColor = Color.BLACK
    private var _gridColor = Color.BLACK
    private var _labelSize = 0f
    private var _labelMargin = 0f
    private var _gridThickness = 2f
    private var _margin = 0f

    // TODO: Does this belong here?
    private var _highlightedPoint: Vector2? = null
    private var _highlightedColor: Int = Color.WHITE
    private var _highlightedSize: Float = 6f

    // X axis
    private var _xLabelCount = 3
    private var _xGridLines = true
    private var _xLabelFormatter: ChartLabelFormatter = NumberChartLabelFormatter()
    private var _xMinimum: Float? = null
    private var _xMaximum: Float? = null

    // Y axis
    private var _yLabelCount = 3
    private var _yGridLines = true
    private var _yLabelFormatter: ChartLabelFormatter = NumberChartLabelFormatter()
    private var _yMinimum: Float? = null
    private var _yMaximum: Float? = null

    // Current values
    private var _currentXMinimum: Float = 0f
    private var _currentXMaximum: Float = 0f
    private var _currentYMinimum: Float = 0f
    private var _currentYMaximum: Float = 0f
    private var _currentChartXMinimum: Float = 0f
    private var _currentChartXMaximum: Float = 0f
    private var _currentChartYMinimum: Float = 0f
    private var _currentChartYMaximum: Float = 0f

    override fun setup() {
        _labelSize = sp(10f)
        _margin = dp(8f)
        _labelMargin = dp(4f)
        _labelColor = Resources.androidTextColorPrimary(context).withAlpha(150)
        _gridColor = Resources.androidTextColorPrimary(context).withAlpha(50)
        updateRange()
    }

    override fun draw() {
        clear()
        background(_backgroundColor)
        // Invalidate all layers if one changed
        if (_layers.any { it.hasChanges }) {
            _layers.forEach { it.invalidate() }
            updateRange()
        }
        updateRange()
        resetChartBounds()
        drawLabelsAndGrid()
        drawData()
    }

    fun deselectPoint() {
        _highlightedPoint = null
        _highlightedColor = Color.WHITE
        _highlightedSize = 6f
        invalidate()
    }

    fun selectPoint(point: Vector2, @ColorInt color: Int, size: Float = 6f) {
        _highlightedPoint = point
        _highlightedColor = color
        _highlightedSize = size
        invalidate()
    }

    private fun drawData() {
        _layers.forEach {
            it.draw(this, this)
        }

        _highlightedPoint?.let {
            ScatterChartLayer(listOf(it), _highlightedColor, _highlightedSize).draw(this, this)
        }
    }

    private fun drawLabelsAndGrid() {
        // Y axis labels
        textSize(_labelSize)
        fill(_labelColor)
        noStroke()
        val yLabels = mutableListOf<Pair<String, Float>>()
        var yLabelSize = 0f
        for (i in 0 until _yLabelCount) {
            val value =
                SolMath.lerp(i / (_yLabelCount - 1).toFloat(), _currentYMinimum, _currentYMaximum)
            val label = _yLabelFormatter.format(value)
            yLabels.add(label to value)
            yLabelSize = max(yLabelSize, textWidth(label))
        }

        // X axis labels
        val xLabels = mutableListOf<Pair<String, Float>>()
        var xLabelSize = 0f
        for (i in 0 until _xLabelCount) {
            val value =
                SolMath.lerp(i / (_xLabelCount - 1).toFloat(), _currentXMinimum, _currentXMaximum)
            val label = _xLabelFormatter.format(value)
            xLabels.add(label to value)
            xLabelSize = max(xLabelSize, textHeight(label))
        }

        _currentChartXMinimum += yLabelSize + if (_yLabelCount > 0f) _labelMargin else 0f
        _currentChartYMaximum -= xLabelSize + if (_xLabelCount > 0f) _labelMargin else 0f

        // Draw y labels
        for (label in yLabels) {
            textAlign(TextAlign.Right)
            val x = yLabelSize
            val y = mapY(label.second) + textHeight(label.first) / 2f
            text(label.first, x, y)
        }

        // Draw x labels
        for (i in xLabels.indices) {
            val label = xLabels[i]
            textAlign(TextAlign.Left)
            val offset = when (i) {
                0 -> 0f
                xLabels.lastIndex -> textWidth(label.first)
                else -> textWidth(label.first) / 2f
            }
            val x = mapX(label.second) - offset
            val y = height.toFloat() - _margin
            text(label.first, x, y)
        }

        // Y grid lines
        if (_yGridLines) {
            noFill()
            stroke(_gridColor)
            strokeWeight(_gridThickness)
            for (label in yLabels) {
                line(
                    _currentChartXMinimum,
                    mapY(label.second),
                    _currentChartXMaximum,
                    mapY(label.second)
                )
            }
        }

        // X grid lines
        if (_xGridLines) {
            noFill()
            stroke(_gridColor)
            strokeWeight(_gridThickness)
            for (label in xLabels) {
                line(
                    mapX(label.second),
                    _currentChartYMinimum,
                    mapX(label.second),
                    _currentChartYMaximum
                )
            }
        }
    }

    private fun resetChartBounds() {
        _currentChartXMinimum = _margin
        _currentChartXMaximum = width.toFloat() - _margin
        _currentChartYMinimum = _margin
        _currentChartYMaximum = height.toFloat() - _margin
    }

    private fun updateRange() {
        _currentXMinimum = _xMinimum ?: Float.POSITIVE_INFINITY
        _currentXMaximum = _xMaximum ?: Float.NEGATIVE_INFINITY
        _currentYMinimum = _yMinimum ?: Float.POSITIVE_INFINITY
        _currentYMaximum = _yMaximum ?: Float.NEGATIVE_INFINITY
        resetChartBounds()

        if (_xMinimum == null || _xMaximum == null || _yMinimum == null || _yMaximum == null) {
            for (d in _layers) {
                for (point in d.data) {
                    if (_xMinimum == null && point.x < _currentXMinimum) {
                        _currentXMinimum = point.x
                    }

                    if (_xMaximum == null && point.x > _currentXMaximum) {
                        _currentXMaximum = point.x
                    }

                    if (_yMinimum == null && point.y < _currentYMinimum) {
                        _currentYMinimum = point.y
                    }

                    if (_yMaximum == null && point.y > _currentYMaximum) {
                        _currentYMaximum = point.y
                    }
                }
            }
        }
    }

    // Map x to view coordinates
    private fun mapX(x: Float): Float {
        return SolMath.map(
            x,
            _currentXMinimum,
            _currentXMaximum,
            _currentChartXMinimum,
            _currentChartXMaximum
        )
    }

    // Map y to view coordinates
    private fun mapY(y: Float): Float {
        return -SolMath.map(
            y,
            _currentYMinimum,
            _currentYMaximum,
            -_currentChartYMaximum,
            -_currentChartYMinimum
        )
    }

    fun plot(data: List<ChartLayer>) {
        _layers = data
        invalidate()
    }

    fun plot(vararg data: ChartLayer) {
        plot(data.toList())
    }

    fun configureXAxis(
        minimum: Float? = null,
        maximum: Float? = null,
        labelCount: Int? = null,
        drawGridLines: Boolean? = null,
        labelFormatter: ChartLabelFormatter? = null
    ) {
        _xMinimum = minimum
        _xMaximum = maximum

        if (labelCount != null) {
            _xLabelCount = labelCount
        }

        if (labelFormatter != null) {
            _xLabelFormatter = labelFormatter
        }

        if (drawGridLines != null) {
            _xGridLines = drawGridLines
        }

        invalidate()
    }

    fun configureYAxis(
        minimum: Float? = null,
        maximum: Float? = null,
        labelCount: Int? = null,
        drawGridLines: Boolean? = null,
        labelFormatter: ChartLabelFormatter? = null
    ) {
        _yMinimum = minimum
        _yMaximum = maximum

        if (labelCount != null) {
            _yLabelCount = labelCount
        }

        if (labelFormatter != null) {
            _yLabelFormatter = labelFormatter
        }

        if (drawGridLines != null) {
            _yGridLines = drawGridLines
        }

        invalidate()
    }

    override fun toPixel(data: Vector2): PixelCoordinate {
        val x = mapX(data.x)
        val y = mapY(data.y)
        return PixelCoordinate(x, y)
    }

    override fun toData(pixel: PixelCoordinate): Vector2 {
        val x = SolMath.map(
            pixel.x,
            _currentChartXMinimum,
            _currentChartXMaximum,
            _currentXMinimum,
            _currentXMaximum
        )
        val y = SolMath.map(
            -pixel.y,
            -_currentChartYMaximum,
            -_currentChartYMinimum,
            _currentYMinimum,
            _currentYMaximum
        )
        return Vector2(x, y)
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = PixelCoordinate(e.x, e.y)

            // TODO: Choose nearest data point rather than layer order?
            for (layer in _layers.reversed()) {
                if (layer.onClick(drawer, this@Chart, pixel)) {
                    break
                }
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
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

        fun indexedData(values: List<Float>): List<Vector2> {
            return values.mapIndexed { index, value ->
                Vector2(index.toFloat(), value)
            }
        }

        fun getYRange(
            data: List<Vector2>,
            margin: Float,
            minRange: Float
        ): Range<Float> {
            val values = data.map { it.y }
            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            return getRange(minValue, maxValue, margin, minRange)
        }

        fun getRange(
            minimum: Float,
            maximum: Float,
            margin: Float,
            minRange: Float
        ): Range<Float> {
            val middle = (minimum + maximum) / 2f
            val start = min(minimum - margin, middle - minRange / 2)
            val end = max(maximum + margin, middle + minRange / 2)

            return Range(start, end)
        }

    }

}

