package com.kylecorry.trail_sense.ui.astronomy

import android.view.View
import com.kylecorry.trail_sense.ui.IStackedBarChart
import java.time.Duration
import java.time.LocalTime

class DayTimeChart(private val chart: IStackedBarChart) {

    fun display(times: List<LocalTime>, colors: List<Int>){
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

        chart.plot(durations, colors)
    }

}