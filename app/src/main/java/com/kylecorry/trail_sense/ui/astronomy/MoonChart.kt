package com.kylecorry.trail_sense.ui.astronomy

import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.moon.MoonStateCalculator
import com.kylecorry.trail_sense.astronomy.moon.MoonTimes
import java.time.LocalTime

class MoonChart(private val chart: BarChart, cursors: List<View>) {

    private val timeChart = DayTimeChart(chart, cursors)

    fun display(moonTimes: MoonTimes, current: LocalTime = LocalTime.now()){
        val times = mutableListOf<LocalTime>()

        if (moonTimes.up != null) {
            times.add(moonTimes.up.toLocalTime())
        }
        if (moonTimes.down != null){
            times.add(moonTimes.down.toLocalTime())
        }

        val isUp = MoonStateCalculator().isUp(moonTimes, LocalTime.MIN)

        val colors = if (isUp){
            listOf(R.color.moon_up, R.color.moon_down)
        } else {
            listOf(R.color.moon_down, R.color.moon_up)
        }

        timeChart.display(times, colors.map { chart.context.resources.getColor(it, null) }, current)
    }

}