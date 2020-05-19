package com.kylecorry.trail_sense.weather.ui

import android.graphics.Color
import android.util.TypedValue
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.domain.PressureUnits
import kotlin.math.max
import kotlin.math.min


class PressureChart(private val chart: LineChart, private val color: Int) {

    private var minRange = MIN_RANGE
    private var granularity = 1f

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.xAxis.setDrawLabels(false)
        chart.axisRight.setDrawLabels(false)

        val theme = chart.context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val arr = chart.context.obtainStyledAttributes(typedValue.data, IntArray(1) {
            android.R.attr.textColorPrimary
        })
        val primaryColor = arr.getColor(0, -1)
        val r = primaryColor.red / 255f
        val g = primaryColor.green / 255f
        val b = primaryColor.blue / 255f
        arr.recycle()

        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.valueOf(r, g, b, 0.2f).toArgb()
        chart.axisLeft.textColor = Color.valueOf(r, g, b, 0.6f).toArgb()
        chart.axisLeft.setLabelCount(3, true)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.setNoDataText("")
    }

    fun setUnits(units: PressureUnits) {
        minRange = PressureUnitUtils.convert(MIN_RANGE, units)
        granularity = PressureUnitUtils.convert(1f, units).roundPlaces(2)
    }

    fun plot(data: List<Pair<Number, Number>>) {
        val values = data.map { Entry(it.first.toFloat(), it.second.toFloat()) }

        val pressures = data.map { it.second.toFloat() }
        var minPressure = pressures.min() ?: 0f
        var maxPressure = pressures.max() ?: 0f
        val middle = (minPressure + maxPressure) / 2f
        minPressure = min(minPressure - granularity, middle - minRange / 2)
        maxPressure = max(maxPressure + granularity, middle + minRange / 2)

        chart.axisLeft.axisMinimum = minPressure
        chart.axisLeft.axisMaximum = maxPressure
        chart.axisLeft.granularity = granularity

        val set1 = LineDataSet(values, chart.context.getString(R.string.pressure_chart_title))
        set1.color = color
        set1.fillAlpha = 180
        set1.lineWidth = 3f
        set1.setDrawValues(false)
        set1.fillColor = color
        set1.setCircleColor(color)
        set1.setDrawCircleHole(false)
        set1.setDrawCircles(true)
        set1.circleRadius = 1.5f
        set1.setDrawFilled(false)


        val lineData = LineData(set1)
        chart.data = lineData
        chart.legend.isEnabled = false
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    companion object {
        const val MIN_RANGE = 14f
    }
}