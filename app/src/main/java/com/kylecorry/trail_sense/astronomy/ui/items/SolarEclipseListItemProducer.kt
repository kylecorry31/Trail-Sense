package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.ui.format.EclipseFormatter
import java.time.LocalDate

class SolarEclipseListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
    ): ListItem? = onDefault {
        val eclipse = astronomyService.getSolarEclipse(location, date) ?: return@onDefault null

        listItem(
            5,
            context.getString(R.string.solar_eclipse),
            EclipseFormatter.type(context, eclipse),
            timeRange(eclipse.start, eclipse.end, date),
            ResourceListIcon(
                if (eclipse.isTotal) {
                    R.drawable.ic_total_solar_eclipse
                } else {
                    R.drawable.ic_partial_solar_eclipse
                }
            )
        ) {}
    }


}