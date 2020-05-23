package com.kylecorry.trail_sense.astronomy.domain.sun

import com.kylecorry.trail_sense.shared.Coordinate
import org.threeten.bp.LocalDate

interface ISunTimesCalculator {

    fun calculate(coordinate: Coordinate, date: LocalDate): SunTimes

}