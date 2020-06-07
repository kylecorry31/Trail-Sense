package com.kylecorry.trail_sense.astronomy.domain.sun

import com.kylecorry.trail_sense.shared.domain.Coordinate
import java.time.LocalDate

interface ISunTimesCalculator {

    fun calculate(coordinate: Coordinate, date: LocalDate): SunTimes

}