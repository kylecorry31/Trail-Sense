package com.kylecorry.trail_sense.astronomy.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import java.time.Instant


class AstroChart(private val chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))
    private var startTime = Instant.now()

    init {
        simpleChart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = -100f,
            maximum = 100f,
        )

        simpleChart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = SimpleLineChart.hourLabelFormatter(chart.context) { startTime }
        )
    }

    fun getPoint(datasetIdx: Int, entryIdx: Int): PixelCoordinate {
        val point = tryOrDefault(SimpleLineChart.Point(datasetIdx, entryIdx, 0f, 0f)) {
            simpleChart.getPoint(datasetIdx, entryIdx)
        }
        return PixelCoordinate(point.x + chart.x, point.y + chart.y)
    }

    fun plot(datasets: List<AstroChartDataset>) {
        val first = datasets.firstOrNull()?.data?.firstOrNull()?.time
        startTime = first
        val sets = datasets.map { set ->
            val values = SimpleLineChart.getDataFromReadings(set.data, startTime) { it }
            SimpleLineChart.Dataset(values, set.color)
        }

        simpleChart.plot(sets)
    }

    data class AstroChartDataset(val data: List<Reading<Float>>, val color: Int)
}