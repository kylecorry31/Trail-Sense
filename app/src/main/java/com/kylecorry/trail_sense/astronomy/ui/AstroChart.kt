package com.kylecorry.trail_sense.astronomy.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.MPPointD
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstroAltitude
import com.kylecorry.trail_sense.shared.toZonedDateTime
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime


class AstroChart(private val chart: LineChart) {

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.setGridBackgroundColor(chart.resources.getColor(R.color.colorAccent, null))
        chart.setDrawGridBackground(true)
        // TODO: Draw background
        chart.setDrawBorders(false)

        chart.xAxis.setDrawLabels(false)
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
        val colors = datasets.map { it.color }.toMutableList()
        val values = datasets.map { it.data.map {
            val date = it.time.toZonedDateTime()
            Pair(((date.toEpochSecond() + date.offset.totalSeconds) * 1000) as Number, it.altitudeDegrees)
        } }.toMutableList()

        val minValue = (values.map{ it.minBy { it.second }?.second ?: 0f }.min() ?: 0f).coerceAtMost(-1f)

        chart.axisLeft.axisMinimum = minValue

        values.add(0, listOf(
            Pair(values.first().first().first, minValue),
            Pair(values.first().last().first, minValue)
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