package com.kylecorry.trail_sense.tools.backtrack.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.paths.PathPoint
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.SimpleLineChart

class PathElevationChart(chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private var granularity = 10f

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)

    private val units = UserPreferences(chart.context).baseDistanceUnits
    private val formatter = FormatService(chart.context)

    private var _path = emptyList<PathPoint>()
    private var _elevationIndex = emptyList<Int>()
    private var _elevations = emptyList<Pair<Float, Float>>()
    private var _listener: (PathPoint) -> Unit = {}

    init {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 3,
            drawGridLines = true,
            labelFormatter = {
                val distance = Distance.meters(it).convertTo(units)
                formatter.formatDistance(
                    distance,
                    Units.getDecimalPlaces(distance.units),
                    false
                )
            }
        )

        simpleChart.configureXAxis(
            labelCount = 4,
            drawGridLines = false,
            labelFormatter = {
                val distance = Distance.meters(it).convertTo(units).toRelativeDistance()
                formatter.formatDistance(
                    distance,
                    Units.getDecimalPlaces(distance.units),
                    false
                )
            }
        )

        simpleChart.setOnValueSelectedListener {
            it ?: return@setOnValueSelectedListener
            if (it.pointIndex != -1) {
                tryOrNothing {
                    val point = _path[_elevationIndex[it.pointIndex]]
                    _listener.invoke(point)
                }
            }
        }
    }

    fun setOnPointClickListener(listener: (point: PathPoint) -> Unit) {
        _listener = listener
    }

    fun plot(path: List<PathPoint>) {
        val elevations = getElevationPlotPoints(path)
        simpleChart.plot(elevations, color)
    }

    private fun getElevationPlotPoints(path: List<PathPoint>): List<Pair<Float, Float>> {
        _path = path
        val elevationIndex = mutableListOf<Int>()
        val points = mutableListOf<Pair<Float, Float>>()
        var distance = 0f
        path.forEachIndexed { index, point ->
            if (index != 0) {
                distance += path[index - 1].coordinate.distanceTo(point.coordinate)
            }

            if (point.elevation != null) {
                points.add(distance to point.elevation)
                elevationIndex.add(index)
            }
        }
        _elevationIndex = elevationIndex
        _elevations = points
        return points
    }
}