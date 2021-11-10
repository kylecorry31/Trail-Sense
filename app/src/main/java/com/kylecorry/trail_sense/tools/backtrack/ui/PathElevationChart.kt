package com.kylecorry.trail_sense.tools.backtrack.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
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

    private val color = Resources.color(chart.context, R.color.colorPrimary)

    private val units = UserPreferences(chart.context).baseDistanceUnits
    private val formatter = FormatService(chart.context)

    init {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 3,
            drawGridLines = true,
            labelFormatter = {
                val distance = Distance.meters(it).convertTo(units)
                formatter.formatDistance(
                    distance,
                    Units.getDecimalPlaces(distance.units)
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
                    Units.getDecimalPlaces(distance.units)
                )
            }
        )

        simpleChart.setOnValueSelectedListener {
            // TODO: Figure out which point was clicked and emit
        }
    }

    fun plot(path: List<PathPoint>) {
        val elevations = getElevationPlotPoints(path)
        // TODO: Add X axis labels
        simpleChart.plot(elevations, color)
    }

    private fun getElevationPlotPoints(path: List<PathPoint>): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        var distance = 0f
        path.forEachIndexed { index, point ->
            if (index != 0){
                distance += path[index - 1].coordinate.distanceTo(point.coordinate)
            }

            if (point.elevation != null){
                points.add(distance to point.elevation)
            }
        }
        return points
    }
}