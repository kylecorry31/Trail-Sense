package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import java.time.LocalDate

class Conditional(val base: AstroFieldProvider, val shouldShow: () -> Boolean) :
    AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        return if (shouldShow()) {
            base.getFields(date, location)
        } else {
            emptyList()
        }
    }
}