package com.kylecorry.trail_sense.tools.tides.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.norm
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.hoursUntil
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import java.time.Instant
import java.time.LocalTime


class TideChart(private val chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private val color = AppColor.Blue.color

    private val formatter = FormatService(chart.context)

    private var startTime = Instant.now()

    init {
        simpleChart.configureYAxis(
            labelCount = 0,
            drawGridLines = false,
            minimum = 0f,
            maximum = 1f,
        )

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

    fun plot(data: List<Reading<Float>>, range: Range<Float>) {
        val first = data.firstOrNull()?.time
        startTime = first
        val values = data.map {
            first!!.hoursUntil(it.time) to
                    norm(it.value, range.start - 0.5f, range.end + 0.5f)
        }
        simpleChart.plot(values, color, filled = true)
    }

    fun getPoint(index: Int): PixelCoordinate {
        val point = try {
            simpleChart.getPoint(0, index)
        } catch (e: Exception) {
            SimpleLineChart.Point(0, index, 0f, 0f)
        }

        return PixelCoordinate(point.x + chart.x, point.y + chart.y)
    }
}