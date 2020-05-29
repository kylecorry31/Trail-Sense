package com.kylecorry.trail_sense.astronomy.domain.moon

import com.kylecorry.trail_sense.shared.Coordinate
import java.time.LocalDate

interface IMoonTimesCalculator {

    fun calculate(location: Coordinate, date: LocalDate = LocalDate.now()): MoonTimes

}