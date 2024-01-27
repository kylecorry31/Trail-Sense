package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import java.time.LocalDate

class MoonListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
    ): ListItem = onDefault {
        // At a glance
        val times = astronomyService.getMoonTimes(location, date)
        val phase = if (date == LocalDate.now()) {
            astronomyService.getCurrentMoonPhase()
        } else {
            astronomyService.getMoonPhase(date)
        }

        // Advanced
        val isSuperMoon = astronomyService.isSuperMoon(date)
        val peak = times.transit?.let { astronomyService.getMoonAltitude(location, it) }

        list(
            2,
            context.getString(R.string.moon),
            percent(formatter.formatMoonPhase(phase.phase), phase.illumination),
            ResourceListIcon(MoonPhaseImageMapper().getPhaseImage(phase.phase)),
            data = riseSetTransit(times)
        ) {
            val advancedData = listOf(
                context.getString(R.string.times) to riseSetTransit(times),
                context.getString(R.string.moon_phase) to data(formatter.formatMoonPhase(phase.phase)),
                context.getString(R.string.illumination) to percent(phase.illumination),
                context.getString(R.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                context.getString(R.string.supermoon) to data(formatter.formatBooleanYesNo(isSuperMoon))
            )

            showAdvancedData(context.getString(R.string.moon), advancedData)
        }
    }


}