package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.MoonPhaseAstroField
import java.time.LocalDate

class MoonPhaseProvider : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val phase = if (date == LocalDate.now()) {
            astronomyService.getCurrentMoonPhase()
        } else {
            astronomyService.getMoonPhase(date)
        }
        val isSupermoon = astronomyService.isSuperMoon(date)
        return listOf(MoonPhaseAstroField(phase, isSupermoon))
    }
}