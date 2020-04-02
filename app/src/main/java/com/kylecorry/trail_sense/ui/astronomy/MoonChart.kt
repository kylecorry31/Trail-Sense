package com.kylecorry.trail_sense.ui.astronomy

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.moon.MoonStateCalculator
import com.kylecorry.trail_sense.astronomy.moon.MoonTimes
import com.kylecorry.trail_sense.ui.IStackedBarChart
import java.time.LocalTime

class MoonChart(private val ctx: Context, chart: IStackedBarChart) {

    private val timeChart = DayTimeChart(chart)

    fun display(moonTimes: MoonTimes){
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

        timeChart.display(times, colors.map { ctx.resources.getColor(it, null) })
    }

}