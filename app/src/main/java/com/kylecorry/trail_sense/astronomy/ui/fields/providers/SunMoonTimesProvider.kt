package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.*
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import java.time.LocalDate

class SunMoonTimesProvider(private val showNoon: Boolean) :
    AstroFieldProvider {
    override fun getFields(date: LocalDate, location: Coordinate): List<AstroField> {
        val astronomyService = AstronomyService()
        val sunTimes = astronomyService.getSunTimes(location, SunTimesMode.Actual, date)
        val moonTimes = astronomyService.getMoonTimes(location, date)

        val times = listOf(
            SunMoonFieldType.Sunrise to sunTimes.rise,
            SunMoonFieldType.Sunset to sunTimes.set,
            SunMoonFieldType.SolarNoon to if (showNoon) sunTimes.transit else null,
            SunMoonFieldType.Moonrise to moonTimes.rise,
            SunMoonFieldType.Moonset to moonTimes.set,
            SunMoonFieldType.LunarNoon to if (showNoon) moonTimes.transit else null,
        ).filterNot { it.second == null || it.second?.toLocalDate() != date }.sortedBy { it.second }
            .map { it.first to it.second!!.toLocalTime() }

        return times.map {
            when (it.first) {
                SunMoonFieldType.Sunrise -> SunriseAstroField(
                    it.second,
                    SunTimesMode.Actual
                )
                SunMoonFieldType.Sunset -> SunsetAstroField(
                    it.second,
                    SunTimesMode.Actual
                )
                SunMoonFieldType.SolarNoon -> {
                    val altitude = astronomyService.getSunAltitude(location, it.second.atDate(date))
                    SolarNoonAstroField(it.second, altitude)
                }
                SunMoonFieldType.Moonrise -> MoonriseAstroField(it.second)
                SunMoonFieldType.Moonset -> MoonsetAstroField(it.second)
                SunMoonFieldType.LunarNoon -> {
                    val altitude =
                        astronomyService.getMoonAltitude(location, it.second.atDate(date))
                    LunarNoonAstroField(it.second, altitude)
                }
            }
        }
    }

    private enum class SunMoonFieldType {
        Sunrise,
        Sunset,
        SolarNoon,
        Moonrise,
        Moonset,
        LunarNoon
    }

}