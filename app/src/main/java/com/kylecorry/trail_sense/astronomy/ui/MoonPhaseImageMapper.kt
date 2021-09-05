package com.kylecorry.trail_sense.astronomy.ui

import androidx.annotation.DrawableRes
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.trail_sense.R

class MoonPhaseImageMapper {

    @DrawableRes
    fun getPhaseImage(phase: MoonTruePhase): Int {
        return when (phase) {
            MoonTruePhase.FirstQuarter -> R.drawable.ic_moon_first_quarter
            MoonTruePhase.Full -> R.drawable.ic_moon
            MoonTruePhase.ThirdQuarter -> R.drawable.ic_moon_third_quarter
            MoonTruePhase.New -> R.drawable.ic_moon_new
            MoonTruePhase.WaningCrescent -> R.drawable.ic_moon_waning_crescent
            MoonTruePhase.WaningGibbous -> R.drawable.ic_moon_waning_gibbous
            MoonTruePhase.WaxingCrescent -> R.drawable.ic_moon_waxing_crescent
            MoonTruePhase.WaxingGibbous -> R.drawable.ic_moon_waxing_gibbous
        }
    }

}