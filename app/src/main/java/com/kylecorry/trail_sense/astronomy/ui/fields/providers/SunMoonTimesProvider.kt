package com.kylecorry.trail_sense.astronomy.ui.fields.providers

import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.ui.fields.*
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
            .map { it.first to it.second!! }

        return times.map {
            when (it.first) {
                SunMoonFieldType.Sunrise -> SunriseAstroField(
                    it.second.toLocalTime(),
                    SunTimesMode.Actual
                )
                SunMoonFieldType.Sunset -> SunsetAstroField(
                    it.second.toLocalTime(),
                    SunTimesMode.Actual
                )
                SunMoonFieldType.SolarNoon -> {
                    val altitude = astronomyService.getSunAltitude(location, it.second)
                    SolarNoonAstroField(it.second.toLocalTime(), altitude)
                }
                SunMoonFieldType.Moonrise -> MoonriseAstroField(it.second.toLocalTime())
                SunMoonFieldType.Moonset -> MoonsetAstroField(it.second.toLocalTime())
                SunMoonFieldType.LunarNoon -> {
                    val altitude =
                        astronomyService.getMoonAltitude(location, it.second)
                    LunarNoonAstroField(it.second.toLocalTime(), altitude)
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