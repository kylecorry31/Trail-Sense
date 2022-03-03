package com.kylecorry.trail_sense.astronomy.ui.commands

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IsTodaySpecification
import java.time.Instant
import java.time.ZonedDateTime

class CenteredAstroChartDataProvider(
    private val astronomy: AstronomyService = AstronomyService(),
    private val isToday: Specification<Instant> = IsTodaySpecification()
) : IAstroChartDataProvider {

    override fun get(location: Coordinate, time: ZonedDateTime): AstroChartData {
        if (isToday.isSatisfiedBy(time.toInstant())) {
            val moon = astronomy.getCenteredMoonAltitudes(
                location,
                time
            )
            val sun = astronomy.getCenteredSunAltitudes(
                location,
                time
            )
            return AstroChartData(sun, moon)
        }
        return DailyAstroChartDataProvider().get(location, time)
    }

}