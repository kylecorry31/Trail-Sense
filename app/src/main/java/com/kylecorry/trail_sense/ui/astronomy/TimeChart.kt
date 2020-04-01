package com.kylecorry.trail_sense.ui.astronomy

import com.kylecorry.trail_sense.ui.IStackedBarChart
import java.time.Duration
import java.time.LocalDateTime

class TimeChart(private val chart: IStackedBarChart, private val displayDuration: Duration) {

    fun display(startTime: LocalDateTime, times: List<LocalDateTime>, colors: List<Int>){
        val sortedTimes = times.sorted()

        val timesUntil = sortedTimes
            .map { Duration.between(startTime, it) }
            .map { if (it <= displayDuration) it else displayDuration }
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