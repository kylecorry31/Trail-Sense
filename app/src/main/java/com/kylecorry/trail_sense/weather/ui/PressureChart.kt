package com.kylecorry.trail_sense.weather.ui

import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.system.UiUtils
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.domain.PressureUnits
import java.time.Duration
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class PressureChart(private val chart: LineChart, private val color: Int, private val selectionListener: IPressureChartSelectedListener? = null) {

    private var minRange = MIN_RANGE
    private var granularity = 1f

    init {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
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
        chart.axisLeft.setLabelCount(5, true)
        chart.axisRight.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.setNoDataText("")

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
                selectionListener?.onNothingSelected()
            }

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e ?: return
                val seconds = e.x * 60 * 60
                val duration = Duration.ofSeconds(seconds.absoluteValue.toLong())
                selectionListener?.onValueSelected(duration, e.y)
            }

        })
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

        val set1 = LineDataSet(values, "Pressure")
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
        const val MIN_RANGE = 40f
    }
}