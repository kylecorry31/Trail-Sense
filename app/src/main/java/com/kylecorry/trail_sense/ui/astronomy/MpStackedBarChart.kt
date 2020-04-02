package com.kylecorry.trail_sense.ui.astronomy

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*


class MpStackedBarChart(private val chart: BarChart):
    IStackedBarChart {

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(true)
        chart.setDrawBorders(false)
        chart.xAxis.isEnabled = false
        chart.xAxis.spaceMin = 0f
        chart.xAxis.spaceMax = 0f
        chart.axisLeft.spaceBottom = 0f
        chart.axisLeft.spaceTop = 0f
        chart.axisLeft.spaceMin = 0f
        chart.axisLeft.spaceMax = 0f
        chart.axisRight.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.legend.isEnabled = false
        chart.minOffset = 0f


        chart.setViewPortOffsets(0f, 0f, 0f, 0f)
        chart.invalidate()

        chart.setDrawValueAboveBar(false)
        chart.isHighlightFullBarEnabled = false
    }

    override fun plot(data: List<Number>, colors: List<Int>) {
        val barEntries = FloatArray(data.size)
        for (i in data.indices){
            barEntries[i] = data[i].toFloat()
        }

        val set1 = BarDataSet(listOf(BarEntry(0f, barEntries)), "Series 1")
        set1.setDrawIcons(false)
        set1.colors = colors
        set1.setDrawValues(false)
        set1.barBorderWidth = 0f
        set1.formLineWidth = 0f
        val barData = BarData(listOf(set1))
        chart.data = barData
        chart.legend.isEnabled = false
        chart.notifyDataSetChanged()
        chart.setFitBars(true)
        chart.invalidate()
    }
}