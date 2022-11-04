package com.kylecorry.trail_sense.weather.ui

import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.shared.views.chart.data.ChartLayer
import com.kylecorry.trail_sense.shared.views.chart.data.LineChartLayer
import com.kylecorry.trail_sense.shared.views.chart.label.HourChartLabelFormatter
import com.kylecorry.trail_sense.shared.views.chart.label.NumberChartLabelFormatter
import java.time.Duration
import java.time.Instant


class PressureChart(
    private val chart: Chart,
    private val selectionListener: ((timeAgo: Duration?, pressure: Float?) -> Unit)? = null
) {

    private var startTime = Instant.now()

    private var minRange = MIN_RANGE
    private var precision = 1
    private var margin = 1f
    private var clickable = selectionListener != null

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)

    init {
        chart.configureYAxis(
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = NumberChartLabelFormatter(precision)
        )

        chart.configureXAxis(
            labelCount = 7,
            drawGridLines = false,
            labelFormatter = HourChartLabelFormatter(chart.context) { startTime }
        )
    }

    private fun onClick(value: Vector2): Boolean {
        if (!clickable || selectionListener == null) {
            return false
        }
        val seconds = value.x * 60 * 60
        val duration = Duration.between(startTime.plusSeconds(seconds.toLong()), Instant.now())
        selectionListener.invoke(duration, value.y)
        chart.selectPoint(
            value,
            Resources.androidTextColorPrimary(chart.context),
            Resources.dp(chart.context, 8f)
        )
        return true
    }

    private fun setUnits(units: PressureUnits) {
        minRange = Pressure.hpa(MIN_RANGE).convertTo(units).pressure
        precision = (Units.getDecimalPlaces(units) - 1).coerceAtLeast(0)
        margin = Pressure.hpa(1f).convertTo(units).pressure.roundPlaces(2)
    }

    fun plot(
        data: List<Reading<Pressure>>,
        raw: List<Reading<Pressure>>? = null
    ) {
        startTime = data.firstOrNull()?.time ?: Instant.now()
        setUnits(data.firstOrNull()?.value?.units ?: PressureUnits.Hpa)
        val values = Chart.getDataFromReadings(data, startTime) {
            it.pressure
        }

        val range = Chart.getYRange(values, margin, minRange)
        // TODO: Support minimum range
        chart.configureYAxis(
            minimum = range.start,
            maximum = range.end,
            labelCount = 5,
            drawGridLines = true,
            labelFormatter = NumberChartLabelFormatter(precision)
        )

        val layers = mutableListOf<ChartLayer>()

        if (raw != null) {
            val rawValues = Chart.getDataFromReadings(raw, startTime) {
                it.pressure
            }
            layers.add(LineChartLayer(rawValues, AppColor.Gray.color.withAlpha(50)))
        }

        layers.add(LineChartLayer(values, color) { onClick(it) })

        chart.plot(layers)
    }

    companion object {
        const val MIN_RANGE = 40f
    }
}