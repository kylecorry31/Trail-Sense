package com.kylecorry.trail_sense.astronomy.domain.moon

import com.kylecorry.trail_sense.shared.Coordinate
import org.threeten.bp.LocalDate

interface IMoonTimesCalculator {

    fun calculate(location: Coordinate, date: LocalDate = LocalDate.now()): MoonTimes

}