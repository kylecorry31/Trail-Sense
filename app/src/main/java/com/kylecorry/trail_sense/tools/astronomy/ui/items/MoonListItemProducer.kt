package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import java.time.LocalDate
import java.time.ZoneId

class MoonListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): ListItem = onDefault {
        // At a glance
        val times = astronomyService.getMoonTimes(location, date)
        val phase = if (date == LocalDate.now()) {
            astronomyService.getCurrentMoonPhase()
        } else {
            astronomyService.getMoonPhase(date)
        }
        val tilt = if (date == LocalDate.now()) {
            astronomyService.getMoonTilt(location)
        } else {
            astronomyService.getMoonTilt(
                location,
                date.atTime(12, 0).atZone(ZoneId.systemDefault()),
                useNearestTransit = true
            )
        }

        // Advanced
        val isSuperMoon = astronomyService.isSuperMoon(date)
        val peak = times.transit?.let { astronomyService.getMoonAltitude(location, it) }
        val azimuth =
            if (date == LocalDate.now()) DeclinationUtils.fromTrueNorthBearing(
                astronomyService.getMoonAzimuth(location),
                declination
            ) else null
        val altitude =
            if (date == LocalDate.now()) astronomyService.getMoonAltitude(location) else null

        list(
            2,
            context.getString(R.string.moon),
            percent(formatter.formatMoonPhase(phase.phase), phase.illumination),
            ResourceListIcon(MoonPhaseImageMapper().getPhaseImage(phase.phase), rotation = tilt),
            data = riseSetTransit(times)
        ) {
            val advancedData = listOf(
                context.getString(R.string.times) to riseSetTransit(times),
                context.getString(R.string.moon_phase) to data(formatter.formatMoonPhase(phase.phase)),
                context.getString(R.string.illumination) to percent(phase.illumination),
                context.getString(R.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                context.getString(R.string.supermoon) to data(
                    formatter.formatBooleanYesNo(
                        isSuperMoon
                    )
                ),
                context.getString(R.string.astronomy_altitude) to altitude?.let { degrees(it) },
                context.getString(R.string.direction) to azimuth?.let { degrees(it.value) }
            )

            showAdvancedData(context.getString(R.string.moon), advancedData)
        }
    }


}