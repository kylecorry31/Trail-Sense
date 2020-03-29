package com.kylecorry.trail_sense.astronomy.moon

enum class MoonTruePhase(val longName: String, val phaseAngle: Int) {
    New("New Moon", 0),
    WaningCrescent("Waning Crescent", 45),
    ThirdQuarter("Third Quarter", 90),
    WaningGibbous("Waning Gibbous", 135),
    Full("Full Moon", 180),
    WaxingGibbous("Waxing Gibbous", 225),
    FirstQuarter("First Quarter", 270),
    WaxingCrescent("Waxing Crescent", 315)
}