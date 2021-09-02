package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.LunarEclipseField
import java.time.LocalDate

class LunarEclipseProvider : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val lunarEclipse = astronomyService.getLunarEclipse(location, date) ?: return emptyList()
        val start = lunarEclipse.start.toLocalDate()
        val end = lunarEclipse.end.toLocalDate()

        val fields = mutableListOf<AstroField>()

        val altitude = astronomyService.getMoonAltitude(location, lunarEclipse.peak)

        if (start == date) {
            fields.add(LunarEclipseField(lunarEclipse, true, altitude))
        }

        if (end == date) {
            fields.add(LunarEclipseField(lunarEclipse, false, altitude))
        }

        return fields
    }
}