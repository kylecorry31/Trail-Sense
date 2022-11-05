package com.kylecorry.trail_sense.navigation.paths.ui

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.data.AreaChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.ChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.LineChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.ScatterChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.DistanceChartLabelFormatter
import kotlin.math.absoluteValue

class PathElevationChart(private val chart: Chart) {

    private val units = UserPreferences(chart.context).baseDistanceUnits
    private val formatter = FormatService(chart.context)

    private var _path = emptyList<PathPoint>()
    private var _elevationIndex = emptyList<Int>()
    private var _elevations = emptyList<Vector2>()
    private var _listener: (PathPoint) -> Unit = {}

    private val highlight = ScatterChartLayer(
        emptyList(),
        Resources.androidTextColorPrimary(chart.context),
        8f
    )

    init {
        chart.configureYAxis(
            labelCount = 3,
            drawGridLines = true,
            labelFormatter = DistanceChartLabelFormatter(
                formatter,
                DistanceUnits.Meters,
                units,
                false
            )
        )

        chart.configureXAxis(
            labelCount = 4,
            drawGridLines = false,
            labelFormatter = DistanceChartLabelFormatter(
                formatter,
                DistanceUnits.Meters,
                units,
                true
            )
        )
    }

    fun setOnPointClickListener(listener: (point: PathPoint) -> Unit) {
        _listener = listener
    }

    fun plot(path: List<PathPoint>, @ColorInt color: Int) {
        val elevations = getElevationPlotPoints(path)

        val margin = Distance.meters(10f).convertTo(units).distance
        val minRange = Distance.meters(100f).convertTo(units).distance
        val range = Chart.getRange(
            elevations.minOfOrNull { it.first.y } ?: 0f,
            elevations.maxOfOrNull { it.first.y } ?: 0f,
            margin,
            minRange)

        chart.configureYAxis(
            labelCount = 3,
            drawGridLines = true,
            minimum = range.start,
            maximum = range.end,
            labelFormatter = DistanceChartLabelFormatter(
                formatter,
                DistanceUnits.Meters,
                units,
                false
            )
        )

        val layers = mutableListOf<ChartLayer>()

        var currentLayer = mutableListOf<Vector2>()
        var currentSteepness = elevations.firstOrNull()?.second ?: Steepness.Low

        elevations.forEach {
            if (it.second == currentSteepness) {
                currentLayer.add(it.first)
            } else {
                currentLayer.add(it.first)
                layers.add(getSlopeChart(currentLayer, currentSteepness, range.start))
                currentLayer = mutableListOf(it.first)
                currentSteepness = it.second
            }
        }

        if (currentLayer.isNotEmpty()) {
            layers.add(getSlopeChart(currentLayer, currentSteepness, range.start))
        }

        _elevations = elevations.map { it.first }

        layers.add(
            LineChartLayer(
                _elevations,
                color
            ) { point ->
                val pointIndex = _elevations.indexOf(point)
                val idx = _elevationIndex[pointIndex]
                tryOrNothing {
                    _listener.invoke(_path[idx])
                }
                true
            }
        )

        layers.add(highlight)

        chart.plot(layers)
    }

    fun highlight(point: PathPoint) {
        val idx = _path.indexOf(point)
        if (idx == -1) {
            return
        }
        val pointIndex = _elevationIndex.indexOf(idx)
        if (pointIndex == -1) {
            return
        }
        tryOrNothing {
            highlight.data = listOf(_elevations[pointIndex])
        }
    }

    fun removeHighlight() {
        highlight.data = emptyList()
    }

    private fun getSlopeChart(
        data: List<Vector2>,
        steepness: Steepness,
        bottom: Float
    ): ChartLayer {
        return AreaChartLayer(
            data,
            Color.TRANSPARENT,
            getColor(steepness),
            initialFillTo = bottom
        )
    }

    private fun getElevationPlotPoints(path: List<PathPoint>): List<Pair<Vector2, Steepness>> {
        _path = path
        val elevationIndex = mutableListOf<Int>()
        val points = mutableListOf<Pair<Vector2, Steepness>>()
        var distance = 0f
        path.forEachIndexed { index, point ->
            if (index != 0) {
                distance += path[index - 1].coordinate.distanceTo(point.coordinate)
            }

            if (point.elevation != null) {
                points.add(Vector2(distance, point.elevation) to getSteepness(point.slope))
                elevationIndex.add(index)
            }
        }
        _elevationIndex = elevationIndex
        return points
    }

    @ColorInt
    private fun getColor(steepness: Steepness): Int {
        return when (steepness) {
            Steepness.Low -> AppColor.Green.color
            Steepness.Medium -> AppColor.Yellow.color
            Steepness.High -> AppColor.Red.color
        }
    }

    private fun getSteepness(slope: Float): Steepness {
        return if (slope.absoluteValue <= 10f) {
            Steepness.Low
        } else if (slope.absoluteValue <= 25f) {
            Steepness.Medium
        } else {
            Steepness.High
        }
    }

    private enum class Steepness {
        Low,
        Medium,
        High
    }
}