package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
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
        val times = astronomyService.getMoonTimes(location, date)
        val phase = if (date == LocalDate.now()) {
            astronomyService.getCurrentMoonPhase()
        } else {
            astronomyService.getMoonPhase(date)
        }

        listItem(
            2,
            context.getString(R.string.moon),
            percent(formatter.formatMoonPhase(phase.phase), phase.illumination),
            riseSet(
                times.rise,
                times.set
            ),
            ResourceListIcon(MoonPhaseImageMapper().getPhaseImage(phase.phase))
        ) {}
    }


}