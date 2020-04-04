package com.kylecorry.trail_sense.astronomy.ui

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.moon.MoonStateCalculator
import com.kylecorry.trail_sense.astronomy.domain.moon.MoonTimes
import com.kylecorry.trail_sense.astronomy.domain.moon.MoonTruePhase
import java.time.LocalTime

class MoonChart(private val chart: DayTimeChartView) {

    fun setMoonImage(phase: MoonTruePhase){
        val moonImgId = when (phase) {
            MoonTruePhase.FirstQuarter -> R.drawable.moon_first_quarter
            MoonTruePhase.Full -> R.drawable.moon_full
            MoonTruePhase.ThirdQuarter -> R.drawable.moon_last_quarter
            MoonTruePhase.New -> R.drawable.moon_new
            MoonTruePhase.WaningCrescent -> R.drawable.moon_waning_crescent
            MoonTruePhase.WaningGibbous -> R.drawable.moon_waning_gibbous
            MoonTruePhase.WaxingCrescent -> R.drawable.moon_waxing_crescent
            MoonTruePhase.WaxingGibbous -> R.drawable.moon_waxing_gibbous
        }

        chart.setCursorImageResource(moonImgId)
    }

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

        chart.setColors(colors.map { chart.context.resources.getColor(it, null) })
        chart.display(times, current)
    }

}