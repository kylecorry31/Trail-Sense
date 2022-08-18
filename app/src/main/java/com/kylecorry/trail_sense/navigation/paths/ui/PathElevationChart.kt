package com.kylecorry.trail_sense.navigation.paths.ui

import androidx.annotation.ColorInt
import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import kotlin.math.absoluteValue

class PathElevationChart(chart: LineChart, private val showSlope: Boolean) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private var granularity = 10f

    private val units = UserPreferences(chart.context).baseDistanceUnits
    private val formatter = FormatService(chart.context)

    private var _path = emptyList<PathPoint>()
    private var _elevationIndex = emptyList<Int>()
    private var _listener: (PathPoint) -> Unit = {}
    private var _fullDatasetIdx = 0

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
            if (it.pointIndex != -1 && it.datasetIndex == _fullDatasetIdx) {
                val idx = _elevationIndex[it.pointIndex]
                tryOrNothing {
                    val point = _path[idx]
                    _listener.invoke(point)
                }
            }
        }
    }

    fun setOnPointClickListener(listener: (point: PathPoint) -> Unit) {
        _listener = listener
    }

    fun plot(path: List<PathPoint>, @ColorInt color: Int) {
        val elevations = getElevationPlotPoints(path)

        val granularity = Distance.meters(10f).convertTo(units).distance
        val minRange = Distance.meters(100f).convertTo(units).distance
        val range = SimpleLineChart.getRange(elevations.minOfOrNull { it.second } ?: 0f,
            elevations.maxOfOrNull { it.second } ?: 0f,
            granularity,
            minRange)

        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 3,
            drawGridLines = true,
            minimum = range.start,
            maximum = range.end,
            labelFormatter = {
                val distance = Distance.meters(it).convertTo(units)
                formatter.formatDistance(
                    distance,
                    Units.getDecimalPlaces(distance.units),
                    false
                )
            }
        )

        val datasets = mutableListOf<SimpleLineChart.Dataset>()

        if (showSlope) {
            var currentDataset = mutableListOf<Pair<Float, Float>>()
            var currentSteepness = elevations.firstOrNull()?.third ?: Steepness.Low

            elevations.forEach {
                if (it.third == currentSteepness) {
                    currentDataset.add(it.first to it.second)
                } else {
                    currentDataset.add(it.first to it.second)
                    datasets.add(
                        SimpleLineChart.Dataset(
                            currentDataset.toList(),
                            getColor(currentSteepness),
                            true,
                            cubic = false,
                            lineWidth = 0f,
                            isHighlightEnabled = false
                        )
                    )
                    currentDataset = mutableListOf(it.first to it.second)
                    currentSteepness = it.third
                }
            }

            if (currentDataset.isNotEmpty()) {
                datasets.add(
                    SimpleLineChart.Dataset(
                        currentDataset.toList(),
                        getColor(currentSteepness),
                        true,
                        cubic = false,
                        lineWidth = 0f,
                        isHighlightEnabled = false
                    )
                )
            }
        }

        _fullDatasetIdx = datasets.size
        datasets.add(
            SimpleLineChart.Dataset(
                elevations.map { it.first to it.second },
                color,
                cubic = false,
                circles = true
            )
        )

        simpleChart.plot(datasets)
    }

    private fun getElevationPlotPoints(path: List<PathPoint>): List<Triple<Float, Float, Steepness>> {
        _path = path
        val elevationIndex = mutableListOf<Int>()
        val points = mutableListOf<Triple<Float, Float, Steepness>>()
        var distance = 0f
        path.forEachIndexed { index, point ->
            if (index != 0) {
                distance += path[index - 1].coordinate.distanceTo(point.coordinate)
            }

            if (point.elevation != null) {
                points.add(Triple(distance, point.elevation, getSteepness(point.slope)))
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