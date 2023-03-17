package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.DaylightSavingsField
import java.time.LocalDate
import java.time.ZoneId

class DaylightSavingsProvider : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val transitions = Time.getDaylightSavingsTransitions(ZoneId.systemDefault(), date.year)

        val today = transitions.firstOrNull { it.first.toLocalDate() == date } ?: return emptyList()

        val yesterday = Time.getDaylightSavings(
            today.first.minusDays(1)
        )

        val tomorrow = Time.getDaylightSavings(
            today.first.plusDays(1)
        )

        val delta = yesterday.minus(tomorrow)

        return listOf(DaylightSavingsField(delta))
    }

}