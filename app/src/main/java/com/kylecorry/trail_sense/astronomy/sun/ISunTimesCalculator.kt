package com.kylecorry.trail_sense.astronomy.sun

import com.kylecorry.trail_sense.shared.Coordinate
import java.time.LocalDate

interface ISunTimesCalculator {

    fun calculate(coordinate: Coordinate, date: LocalDate): SunTimes

}