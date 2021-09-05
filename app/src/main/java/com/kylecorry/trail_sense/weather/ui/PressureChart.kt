package com.kylecorry.trail_sense.weather.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import java.time.Duration
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class PressureChart(
    chart: LineChart,
    private val selectionListener: ((timeAgo: Duration?, pressure: Float?) -> Unit)? = null
) {


    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private var minRange = MIN_RANGE
    private var granularity = 1f

    private val color = Resources.color(chart.context, R.color.colorPrimary)

    init {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true
        )

        simpleChart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )


        simpleChart.setOnValueSelectedListener {
            if (it == null) {
                selectionListener?.invoke(null, null)
                return@setOnValueSelectedListener
            }
            val seconds = it.first * 60 * 60
            val duration = Duration.ofSeconds(seconds.absoluteValue.toLong())
            selectionListener?.invoke(duration, it.second)
        }
    }

    fun setUnits(units: PressureUnits) {
        minRange = PressureUnitUtils.convert(MIN_RANGE, units)
        granularity = PressureUnitUtils.convert(1f, units).roundPlaces(2)
    }

    fun plot(data: List<Pair<Number, Number>>) {
        val values = data.map { it.first.toFloat() to it.second.toFloat() }

        val pressures = values.map { it.second }
        var minPressure = pressures.minOrNull() ?: 0f
        var maxPressure = pressures.maxOrNull() ?: 0f
        val middle = (minPressure + maxPressure) / 2f
        minPressure = min(minPressure - granularity, middle - minRange / 2)
        maxPressure = max(maxPressure + granularity, middle + minRange / 2)


        simpleChart.configureYAxis(
            minimum = minPressure,
            maximum = maxPressure,
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true
        )

        simpleChart.plot(values, color)
    }

    companion object {
        const val MIN_RANGE = 40f
    }
}