package com.kylecorry.trail_sense.weather.ui

import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlin.math.max
import kotlin.math.min


class TemperatureChart(private val chart: LineChart, private val color: Int) {

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

        val primaryColor = UiUtils.androidTextColorPrimary(chart.context)
        val r = primaryColor.red
        val g = primaryColor.green
        val b = primaryColor.blue

        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.argb(50, r, g, b)
        chart.axisLeft.textColor = Color.argb(150, r, g, b)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.setNoDataText("")
    }

    fun plot(data: List<Float>, units: TemperatureUnits = TemperatureUnits.C) {
        val values = data.mapIndexed { index, value -> Entry(index.toFloat(), value) }

        val minimum = if (units == TemperatureUnits.C) 0f else 32f
        val maximum = if (units == TemperatureUnits.C) 38f else 100f

        chart.axisLeft.axisMinimum = min(minimum, data.minOrNull() ?: minimum)
        chart.axisLeft.axisMaximum = max(maximum, data.maxOrNull() ?: maximum)
        chart.axisLeft.granularity = granularity

        val set1 = LineDataSet(values, "Temperature")
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
}