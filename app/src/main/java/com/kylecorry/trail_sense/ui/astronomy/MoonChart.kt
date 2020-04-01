package com.kylecorry.trail_sense.ui.astronomy

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.moon.MoonTimes
import com.kylecorry.trail_sense.ui.IStackedBarChart
import java.time.Duration
import java.time.LocalDateTime

class MoonChart(private val ctx: Context, chart: IStackedBarChart) {

    private val timeChart = TimeChart(chart, displayDuration)

    fun display(moonTimes: List<MoonTimes>, isUp: Boolean, currentTime: LocalDateTime = LocalDateTime.now()){
        val times = mutableListOf<LocalDateTime>()


        for (t in moonTimes){
            if (t.up != null && t.up.isAfter(currentTime)) {
                times.add(t.up)
            }
            if (t.down != null && t.down.isAfter(currentTime)){
                times.add(t.down)
            }
        }

        val colors = if (isUp){
            listOf(R.color.moon_up, R.color.moon_down)
        } else {
            listOf(R.color.moon_down, R.color.moon_up)
        }

        timeChart.display(currentTime, times, colors.map { ctx.resources.getColor(it, null) })
    }


    companion object {
        private val displayDuration = Duration.ofHours(24)
    }

}