package com.kylecorry.trail_sense.astronomy.domain.moon

private const val truePhaseWidth = 11.25f

enum class MoonTruePhase(val longName: String, val direction: String, val startAngle: Float, val endAngle: Float) {
    New("New Moon", "New", 360 - truePhaseWidth, truePhaseWidth),
    WaningCrescent("Waning Crescent", "Waning", truePhaseWidth, 90 - truePhaseWidth),
    ThirdQuarter("Third Quarter", "Waning", 90 - truePhaseWidth, 90 + truePhaseWidth),
    WaningGibbous("Waning Gibbous", "Waning", 90 + truePhaseWidth, 180 - truePhaseWidth),
    Full("Full Moon", "Full", 180 - truePhaseWidth, 180 + truePhaseWidth),
    WaxingGibbous("Waxing Gibbous", "Waxing", 180 + truePhaseWidth, 270 - truePhaseWidth),
    FirstQuarter("First Quarter", "Waxing", 270 - truePhaseWidth, 270 + truePhaseWidth),
    WaxingCrescent("Waxing Crescent", "Waxing", 270 + truePhaseWidth, 360 - truePhaseWidth)
}