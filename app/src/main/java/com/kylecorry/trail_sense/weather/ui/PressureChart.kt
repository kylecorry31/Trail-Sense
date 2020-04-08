package com.kylecorry.trail_sense.weather.ui

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kylecorry.trail_sense.weather.domain.PressureUnits


class PressureChart(private val chart: LineChart, private val color: Int) {

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.xAxis.setDrawLabels(false)
        chart.axisRight.setDrawLabels(false)

        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.valueOf(0f, 0f, 0f, 0.2f).toArgb()
        chart.axisLeft.textColor = Color.valueOf(0f, 0f, 0f, 0.6f).toArgb()
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.setNoDataText("")
    }

    fun setUnits(units: PressureUnits){
        if (units == PressureUnits.Inhg || units == PressureUnits.Inhg){
            chart.axisLeft.granularity = 0.1f
        } else {
            chart.axisLeft.granularity = 1f
        }
    }

    fun plot(data: List<Pair<Number, Number>>) {
        val values = data.map { Entry(it.first.toFloat(), it.second.toFloat()) }


        val set1 = LineDataSet(values, "Pressure")
        set1.color = color
        set1.fillAlpha = 180
        set1.lineWidth = 3f
        set1.setDrawValues(false)
        set1.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set1.fillColor = color
        set1.setCircleColor(color)
        set1.setDrawCircleHole(false)
        set1.setDrawCircles(false)
        set1.setDrawFilled(false)


        val lineData = LineData(set1)
        chart.data = lineData
        chart.legend.isEnabled = false
        chart.notifyDataSetChanged()
        chart.invalidate()
    }
}