package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.SpacerAstroField
import java.time.LocalDate

class Section(val base: AstroFieldProvider) : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val baseFields = base.getFields(date, location)
        if (baseFields.isEmpty()) {
            return emptyList()
        }

        return listOf(SpacerAstroField()) + baseFields
    }
}