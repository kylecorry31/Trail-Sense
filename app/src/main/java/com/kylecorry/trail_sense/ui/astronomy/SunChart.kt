package com.kylecorry.trail_sense.ui.astronomy

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.sun.SunTimes
import com.kylecorry.trail_sense.ui.IStackedBarChart
import java.time.Duration
import java.time.LocalDateTime

class SunChart(private val ctx: Context, chart: IStackedBarChart) {

    private val timeChart = TimeChart(chart, displayDuration)

    fun display(sunTimes: List<SunTimes>, currentTime: LocalDateTime = LocalDateTime.now()){
        val times = mutableListOf<LocalDateTime>()

        for (t in sunTimes){
            times.add(t.up)
            times.add(t.down)
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

        timeChart.display(currentTime, times, colors.map { ctx.resources.getColor(it, null) })
    }


    companion object {
        private val displayDuration = Duration.ofHours(24)
    }

}