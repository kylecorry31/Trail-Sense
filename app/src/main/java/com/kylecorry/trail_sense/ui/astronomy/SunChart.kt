package com.kylecorry.trail_sense.ui.astronomy

import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.sun.SunTimes
import java.time.LocalTime

class SunChart(private val chart: BarChart, cursors: List<View>) {

    private val timeChart = DayTimeChart(chart, cursors)

    fun display(sunTimes: List<SunTimes>, current: LocalTime = LocalTime.now()){
        val times = mutableListOf<LocalTime>()

        for (t in sunTimes){
            times.add(t.up.toLocalTime())
            times.add(t.down.toLocalTime())
        }

        val colors = mutableListOf(
            R.color.night,
            R.color.astronomical_twilight,
            R.color.nautical_twilight,
            R.color.civil_twilight,
            R.color.day,
            R.color.civil_twilight,
            R.color.nautical_twilight,
            R.color.astronomical_twilight
        )

        timeChart.display(times, colors.map { chart.context.resources.getColor(it, null) }, current)
    }
}