package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.SolarEclipseField
import java.time.LocalDate

class SolarEclipseProvider : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val eclipse = astronomyService.getSolarEclipse(location, date) ?: return emptyList()
        val start = eclipse.start.toLocalDate()
        val end = eclipse.end.toLocalDate()

        val fields = mutableListOf<AstroField>()

        if (start == date) {
            fields.add(SolarEclipseField(eclipse, true))
        }

        if (end == date) {
            fields.add(SolarEclipseField(eclipse, false))
        }

        return fields
    }
}