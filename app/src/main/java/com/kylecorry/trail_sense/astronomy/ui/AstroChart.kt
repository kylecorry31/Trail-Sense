package com.kylecorry.trail_sense.astronomy.ui

import android.graphics.Color
import android.util.TypedValue
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstroAltitude
import com.kylecorry.trail_sense.shared.toZonedDateTime
import com.kylecorry.trail_sense.weather.domain.LowPassFilter


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
        chart.setGridBackgroundColor(chart.resources.getColor(R.color.colorAccent, null))
        chart.setDrawGridBackground(true)
        chart.setDrawBorders(false)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.valueFormatter = TimeLabelFormatter(chart.context)
//        chart.xAxis.labelRotationAngle = 45f
        chart.axisRight.setDrawLabels(false)
        chart.axisLeft.setDrawLabels(false)
        chart.axisLeft.setDrawZeroLine(true)

        val theme = chart.context.theme
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val arr = chart.context.obtainStyledAttributes(typedValue.data, IntArray(1) {
            android.R.attr.textColorPrimary
        })
        val primaryColor = arr.getColor(0, -1)
        val r = primaryColor.red
        val g = primaryColor.green
        val b = primaryColor.blue
        arr.recycle()

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

    fun plot(datasets: List<AstroChartDataset>){
        val filters = datasets.map { LowPassFilter(0.8f, it.data.first().altitudeDegrees) }
        val colors = datasets.map { it.color }.toMutableList()
        val granularity = 10
        val values = datasets.mapIndexed { idx, d -> d.data.mapIndexed { i, a ->
            val time = i * granularity / 60f
            Pair(time as Number, filters[idx].filter(a.altitudeDegrees))
        } }.toMutableList()

        val minValue = (values.map{ it.minBy { it.second }?.second ?: 0f }.min() ?: 0f).coerceAtMost(-1f) - 10f
        val maxValue = (values.map{ it.maxBy { it.second }?.second ?: 0f }.max() ?: 0f).coerceAtLeast(1f) + 10f

        chart.axisLeft.axisMinimum = minValue
        chart.axisLeft.axisMaximum = maxValue

        val start = values.map { it.first().first }.minBy { it.toDouble() }?.toFloat() ?: 0f
        val end = values.map { it.last().first }.maxBy { it.toDouble() }?.toFloat() ?: 0f

        values.add(0, listOf(
            Pair(0, minValue),
            Pair(24, minValue)
        ))

        colors.add(0, chart.resources.getColor(R.color.colorSecondary, null))

        val sets = values.mapIndexed { index, data ->
            val entries = data.mapIndexed { idx, value -> Entry(value.first.toFloat(), value.second) }
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
            if (index == 0){
                set1.setDrawCircles(false)
                set1.lineWidth = 0f
                set1.fillAlpha = 255
                set1.setDrawFilled(true)
            } else {
                set1.setDrawFilled(false)
            }

            if (index == 1){
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

    data class AstroChartDataset(val data: List<AstroAltitude>, val color: Int)
}