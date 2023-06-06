package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
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

        listItem(
            2,
            context.getString(R.string.moon),
            percent(formatter.formatMoonPhase(phase.phase), phase.illumination),
            riseSet(
                times.rise,
                times.set
            ),
            ResourceListIcon(MoonPhaseImageMapper().getPhaseImage(phase.phase))
        ) {
            Alerts.dialog(
                context,
                context.getString(R.string.moon),
                fields(
                    // Moon rise/set
                    context.getString(R.string.times) to riseSet(
                        times.rise,
                        times.set
                    ),

                    // Moon phase
                    context.getString(R.string.moon_phase) to percent(
                        formatter.formatMoonPhase(phase.phase),
                        phase.illumination
                    ),

                    // Super moon
                    context.getString(R.string.supermoon) to formatter.formatBooleanYesNo(
                        isSuperMoon
                    )
                ),
                cancelText = null
            )
        }
    }


}