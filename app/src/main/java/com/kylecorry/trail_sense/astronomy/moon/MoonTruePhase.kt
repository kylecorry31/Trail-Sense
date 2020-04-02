package com.kylecorry.trail_sense.astronomy.moon

private const val truePhaseWidth = 11.25f

enum class MoonTruePhase(val longName: String, val startAngle: Float, val endAngle: Float) {
    New("New Moon", 360 - truePhaseWidth, truePhaseWidth),
    WaningCrescent("Waning Crescent", truePhaseWidth, 90 - truePhaseWidth),
    ThirdQuarter("Third Quarter", 90 - truePhaseWidth, 90 + truePhaseWidth),
    WaningGibbous("Waning Gibbous", 90 + truePhaseWidth, 180 - truePhaseWidth),
    Full("Full Moon", 180 - truePhaseWidth, 180 + truePhaseWidth),
    WaxingGibbous("Waxing Gibbous", 180 + truePhaseWidth, 270 - truePhaseWidth),
    FirstQuarter("First Quarter", 270 - truePhaseWidth, 270 + truePhaseWidth),
    WaxingCrescent("Waxing Crescent", 270 + truePhaseWidth, 360 - truePhaseWidth)
}