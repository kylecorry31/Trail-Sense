package com.kylecorry.trail_sense.astronomy.domain.moon

import com.kylecorry.trail_sense.R

private const val truePhaseWidth = 11.25f

// TODO: Move resource names out of this class
enum class MoonTruePhase(val longNameResource: Int, val startAngle: Float, val endAngle: Float) {
    New(R.string.new_moon, 360 - truePhaseWidth, truePhaseWidth),
    WaningCrescent(R.string.waning_crescent, truePhaseWidth, 90 - truePhaseWidth),
    ThirdQuarter(R.string.third_quarter, 90 - truePhaseWidth, 90 + truePhaseWidth),
    WaningGibbous(R.string.waning_gibbous, 90 + truePhaseWidth, 180 - truePhaseWidth),
    Full(R.string.full_moon, 180 - truePhaseWidth, 180 + truePhaseWidth),
    WaxingGibbous(R.string.waxing_gibbous, 180 + truePhaseWidth, 270 - truePhaseWidth),
    FirstQuarter(R.string.first_quarter, 270 - truePhaseWidth, 270 + truePhaseWidth),
    WaxingCrescent(R.string.waxing_crescent, 270 + truePhaseWidth, 360 - truePhaseWidth)
}