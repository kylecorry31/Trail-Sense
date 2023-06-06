package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import java.time.LocalDate

class SunListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
    ): ListItem = onDefault {
        val times = astronomyService.getSunTimes(location, prefs.astronomy.sunTimesMode, date)
        val daylight = astronomyService.getLengthOfDay(location, prefs.astronomy.sunTimesMode, date)

        listItem(
            1,
            context.getString(R.string.sun),
            context.getString(
                R.string.daylight_duration,
                formatter.formatDuration(daylight, false)
            ),
            riseSet(
                times.rise,
                times.set
            ),
            ResourceListIcon(R.drawable.circle, AppColor.Yellow.color),
        ) {}
    }


}