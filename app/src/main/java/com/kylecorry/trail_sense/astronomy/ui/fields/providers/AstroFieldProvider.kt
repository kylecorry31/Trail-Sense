package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import java.time.LocalDate

interface AstroFieldProvider {

    fun getFields(date: LocalDate, location: Coordinate): List<AstroField>

}