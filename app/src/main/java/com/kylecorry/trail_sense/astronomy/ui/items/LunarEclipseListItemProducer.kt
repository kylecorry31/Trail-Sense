package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.math.DecimalFormatter
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

        // Advanced
        val peakAltitude = astronomyService.getMoonAltitude(location, eclipse.peak)
        val peakAzimuth = astronomyService.getMoonAzimuth(location, eclipse.peak)

        list(
            4,
            context.getString(R.string.lunar_eclipse),
            EclipseFormatter.type(context, eclipse),
            ResourceListIcon(
                if (eclipse.isTotal) {
                    R.drawable.ic_moon_total_eclipse
                } else {
                    R.drawable.ic_moon_partial_eclipse
                }
            ),
            data = times(eclipse.start, eclipse.end, date)
        ) {
            val advancedData = listOf(
                context.getString(R.string.times) to times(
                    eclipse.start,
                    eclipse.end,
                    date
                ),
                context.getString(R.string.duration) to data(
                    formatter.formatDuration(
                        eclipse.duration,
                        false
                    )
                ),
                context.getString(R.string.obscuration) to data(
                    EclipseFormatter.type(
                        context,
                        eclipse
                    )
                ),
                context.getString(R.string.magnitude) to data(
                    DecimalFormatter.format(
                        eclipse.magnitude,
                        2
                    )
                ),
                context.getString(R.string.peak_time) to time(eclipse.peak, date),
                context.getString(R.string.astronomy_altitude_peak) to data(
                    formatter.formatDegrees(
                        peakAltitude
                    )
                ),
                context.getString(R.string.astronomy_direction_peak) to data(
                    formatter.formatDirection(
                        peakAzimuth.direction
                    )
                )
            )

            showAdvancedData(context.getString(R.string.lunar_eclipse), advancedData)
        }
    }


}