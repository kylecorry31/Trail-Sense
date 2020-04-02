package com.kylecorry.trail_sense.ui.astronomy

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.sun.SunTimes
import com.kylecorry.trail_sense.ui.IStackedBarChart
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class SunChart(private val ctx: Context, chart: IStackedBarChart) {

    private val timeChart = DayTimeChart(chart)

    fun display(sunTimes: List<SunTimes>){
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

        timeChart.display(times, colors.map { ctx.resources.getColor(it, null) })
    }
}