package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.AstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.SunriseAstroField
import com.kylecorry.trail_sense.astronomy.ui.fields.SunsetAstroField
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import java.time.LocalDate

class AstronomicalSunTimesProvider : AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val sunTimes = astronomyService.getSunTimes(location, SunTimesMode.Astronomical, date)

        val times = listOf(
            true to sunTimes.rise,
            false to sunTimes.set
        ).filterNot { it.second == null || it.second?.toLocalDate() != date }.sortedBy { it.second }
            .map { it.first to it.second!!.toLocalTime() }

        return times.map {
            if (it.first) {
                SunriseAstroField(
                    it.second,
                    SunTimesMode.Astronomical
                )
            } else {
                SunsetAstroField(
                    it.second,
                    SunTimesMode.Astronomical
                )
            }
        }
    }

}