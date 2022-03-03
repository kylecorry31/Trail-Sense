package com.kylecorry.trail_sense.astronomy.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.hoursUntil
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import java.time.Instant
import java.time.LocalTime


class AstroChart(private val chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))
    private val formatter = FormatService(chart.context)
    private var startTime = Instant.now()

    init {
        simpleChart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = -100f,
            maximum = 100f,
        )

        // TODO: Extract hour formatter
        simpleChart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = {
                val duration = Time.hours(it.toDouble())
                val time = startTime.plus(duration)
                val local = time.toZonedDateTime().toLocalTime()
                val hour = if (local.minute >= 30) {
                    local.hour + 1
                } else {
                    local.hour
                }
                formatter.formatTime(
                    LocalTime.of(hour % 24, 0),
                    includeSeconds = false,
                    includeMinutes = false
                )
            }
        )
    }

    fun getPoint(datasetIdx: Int, entryIdx: Int): PixelCoordinate {
        val point = try {
            simpleChart.getPoint(datasetIdx, entryIdx)
        } catch (e: Exception) {
            SimpleLineChart.Point(datasetIdx, entryIdx, 0f, 0f)
        }

        return PixelCoordinate(point.x + chart.x, point.y + chart.y)
    }

    fun plot(datasets: List<AstroChartDataset>) {
        val first = datasets.firstOrNull()?.data?.firstOrNull()?.time
        startTime = first
        val sets = datasets.map { set ->
            val values = set.data.map {
                first!!.hoursUntil(it.time) to it.value
            }
            SimpleLineChart.Dataset(values, set.color)
        }

        simpleChart.plot(sets)
    }

    data class AstroChartDataset(val data: List<Reading<Float>>, val color: Int)
}