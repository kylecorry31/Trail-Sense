package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.DaylightAstroField
import java.time.LocalDate

class DaylightProvider(val sunTimesMode: SunTimesMode) : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val daylight = astronomyService.getLengthOfDay(location, sunTimesMode, date)
        val season = astronomyService.getSeason(location, date)
        return listOf(DaylightAstroField(daylight, season))
    }
}