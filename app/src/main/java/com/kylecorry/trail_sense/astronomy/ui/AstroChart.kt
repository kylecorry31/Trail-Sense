package com.kylecorry.trail_sense.astronomy.ui

import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.weather.domain.LowPassFilter
import java.time.LocalDateTime


class AstroChart(private val chart: LineChart) {

    val x: Float
        get() = chart.x

    val y: Float
        get() = chart.y

    val width: Int
        get() = chart.width

    val height: Int
        get() = chart.height

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.valueFormatter = TimeLabelFormatter(chart.context)
        chart.axisRight.setDrawLabels(false)
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawZeroLine(false)

        val primaryColor = Resources.androidTextColorPrimary(chart.context)
        val r = primaryColor.red
        val g = primaryColor.green
        val b = primaryColor.blue

        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.textColor = Color.argb(150, r, g, b)
        chart.axisLeft.gridColor = Color.argb(50, r, g, b)
        chart.axisLeft.textColor = Color.argb(150, r, g, b)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.setNoDataText("")
    }

    fun getPoint(datasetIdx: Int, entryIdx: Int): Pair<Float, Float> {
        val entry = chart.lineData.getDataSetByIndex(datasetIdx).getEntryForIndex(entryIdx)
        val point =  chart.getPixelForValues(entry.x, entry.y, AxisDependency.LEFT)
        return Pair(point.x.toFloat() + chart.x, point.y.toFloat() + chart.y)
    }

    fun plot(datasets: List<AstroChartDataset>, startHour: Float = 0f){
        val filters = datasets.map { LowPassFilter(0.8f, it.data.first().second) }
        val colors = datasets.map { it.color }.toMutableList()
        val granularity = 10
        val values = datasets.mapIndexed { idx, d -> d.data.mapIndexed { i, a ->
            val time = startHour + i * granularity / 60f
            Pair(time as Number, filters[idx].filter(a.second))
        } }.toMutableList()

        val minValue = -100f
        val maxValue = 100f

        chart.axisLeft.axisMinimum = minValue
        chart.axisLeft.axisMaximum = maxValue

        val sets = values.mapIndexed { index, data ->
            val entries = data.map { Entry(it.first.toFloat(), it.second) }
            val set1 = LineDataSet(entries, index.toString())
            set1.setDrawIcons(true)
            set1.color = colors[index]
            set1.lineWidth = 2f
            set1.setDrawValues(false)
            set1.fillColor = colors[index]
            set1.setCircleColor(colors[index])
            set1.setDrawCircleHole(false)
            set1.mode = LineDataSet.Mode.CUBIC_BEZIER
            set1.cubicIntensity = 0.005f
            set1.setDrawCircles(false)
            set1.circleRadius = 1f
            set1.setDrawFilled(false)

            if (index == 0){
                set1.valueFormatter = TimeLabelFormatter(chart.context)
            }

            set1
        }

        val lineData = LineData(sets)
        chart.data = lineData
        chart.legend.isEnabled = false
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    data class AstroChartDataset(val data: List<Pair<LocalDateTime, Float>>, val color: Int)
}