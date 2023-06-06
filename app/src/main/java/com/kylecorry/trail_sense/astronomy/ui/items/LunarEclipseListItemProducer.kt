package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.ui.format.EclipseFormatter
import java.time.LocalDate

class LunarEclipseListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
    ): ListItem? = onDefault {
        val eclipse = astronomyService.getLunarEclipse(location, date) ?: return@onDefault null

        listItem(
            4,
            context.getString(R.string.lunar_eclipse),
            EclipseFormatter.type(context, eclipse),
            timeRange(eclipse.start, eclipse.end, date),
            ResourceListIcon(
                if (eclipse.isTotal) {
                    R.drawable.ic_moon_total_eclipse
                } else {
                    R.drawable.ic_moon_partial_eclipse
                }
            )
        ) {}
    }


}