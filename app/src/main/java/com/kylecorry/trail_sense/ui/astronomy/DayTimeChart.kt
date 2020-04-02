package com.kylecorry.trail_sense.ui.astronomy

import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.kylecorry.trail_sense.ui.MpStackedBarChart
import java.time.Duration
import java.time.LocalTime

class DayTimeChart(private val chart: BarChart, private val cursors: List<View>) {

    private val stackedChart = MpStackedBarChart(chart)

    fun display(times: List<LocalTime>, colors: List<Int>, current: LocalTime = LocalTime.now()){
        val sortedTimes = times.sorted().toMutableList()
        sortedTimes.add(LocalTime.MAX)

        val timesUntil = sortedTimes
            .map { Duration.between(LocalTime.MIN, it) }
            .map { if (it.isNegative) 0 else it.seconds }

        val durations = mutableListOf<Long>()

        var cumulativeTime = 0L

        for (time in timesUntil) {
            val dt = time - cumulativeTime
            durations.add(dt)
            cumulativeTime += dt
        }

        val pct = getPercentOfDay(current)

        for (cursor in cursors){
            cursor.x = chart.width * pct + chart.x - cursor.width / 2f
        }

        stackedChart.plot(durations, colors)
    }


    private fun getPercentOfDay(current: LocalTime): Float {
        val duration = Duration.between(LocalTime.MIN, current).seconds
        return duration / Duration.ofDays(1).seconds.toFloat()
    }

}