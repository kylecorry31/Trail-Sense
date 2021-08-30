package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.DaylightAstroField
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import java.time.LocalDate

class DaylightProvider(val sunTimesMode: SunTimesMode) : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val daylight = astronomyService.getLengthOfDay(location, sunTimesMode, date)
        return listOf(DaylightAstroField(daylight))
    }
}