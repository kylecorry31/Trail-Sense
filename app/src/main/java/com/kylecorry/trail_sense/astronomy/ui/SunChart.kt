package com.kylecorry.trail_sense.astronomy.ui

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimes
import java.time.LocalTime

class SunChart(private val chart: DayTimeChartView) {

    init {
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

        chart.setColors(colors.map { chart.context.resources.getColor(it, null) })
    }

    fun display(sunTimes: List<SunTimes>, current: LocalTime = LocalTime.now()){
        val times = mutableListOf<LocalTime>()

        for (t in sunTimes){
            times.add(t.up.toLocalTime())
            times.add(t.down.toLocalTime())
        }

        chart.display(times, current)
    }
}