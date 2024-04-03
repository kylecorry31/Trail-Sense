package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.astronomy.ui.format.EclipseFormatter
import java.time.LocalDate

class LunarEclipseListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): ListItem? = onDefault {
        val eclipse = astronomyService.getLunarEclipse(location, date) ?: return@onDefault null

        // Advanced
        val peakAltitude = astronomyService.getMoonAltitude(location, eclipse.peak)
        val peakAzimuth = DeclinationUtils.fromTrueNorthBearing(
            astronomyService.getMoonAzimuth(location, eclipse.peak),
            declination
        )

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
            data = times(eclipse.start, eclipse.peak, eclipse.end, date)
        ) {
            val advancedData = listOf(
                context.getString(R.string.times) to times(
                    eclipse.start,
                    eclipse.peak,
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