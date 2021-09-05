package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.SunriseAstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.SunsetAstroField
import java.time.LocalDate

class NauticalTimesProvider : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val sunTimes = astronomyService.getSunTimes(location, SunTimesMode.Nautical, date)

        val times = listOf(
            true to sunTimes.rise,
            false to sunTimes.set
        ).filterNot { it.second == null || it.second?.toLocalDate() != date }.sortedBy { it.second }
            .map { it.first to it.second!!.toLocalTime() }

        return times.map {
            if (it.first) {
                SunriseAstroField(
                    it.second,
                    SunTimesMode.Nautical
                )
            } else {
                SunsetAstroField(
                    it.second,
                    SunTimesMode.Nautical
                )
            }
        }
    }

}