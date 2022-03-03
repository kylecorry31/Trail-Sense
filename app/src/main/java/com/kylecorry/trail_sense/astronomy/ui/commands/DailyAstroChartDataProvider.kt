package com.kylecorry.trail_sense.astronomy.ui.commands

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import java.time.ZonedDateTime

class DailyAstroChartDataProvider(private val astronomy: AstronomyService = AstronomyService()) :
    IAstroChartDataProvider {
    override fun get(location: Coordinate, time: ZonedDateTime): AstroChartData {
        val sun = astronomy.getSunAltitudes(location, time.toLocalDate())
        val moon = astronomy.getMoonAltitudes(location, time.toLocalDate())
        return AstroChartData(sun, moon)
    }
}