package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import androidx.core.graphics.drawable.toDrawable
import com.kylecorry.andromeda.views.list.DrawableListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.astronomy.ui.SolarEclipseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.EclipseFormatter
import java.time.LocalDate

class SolarEclipseListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): ListItem? = onDefault {
        val eclipse = astronomyService.getSolarEclipse(location, date) ?: return@onDefault null

        // Advanced
        val sun = astronomyService.getSunPosition(location, eclipse.peak)
        val peakAzimuth = DeclinationUtils.fromTrueNorthBearing(
            sun.azimuth,
            declination
        )
        val moon = astronomyService.getMoonPosition(location, eclipse.peak)

        list(
            5,
            context.getString(R.string.solar_eclipse),
            EclipseFormatter.type(context, eclipse),
            DrawableListIcon(
                SolarEclipseImageMapper(context).getEclipseImage(
                    sun,
                    moon,
                    eclipse.isTotal,
                    imageSize,
                    imageSize
                ).toDrawable(context.resources)
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
                context.getString(R.string.duration) to duration(eclipse.duration),
                context.getString(R.string.obscuration) to data(
                    EclipseFormatter.type(
                        context,
                        eclipse
                    )
                ),
                context.getString(R.string.magnitude) to decimal(eclipse.magnitude, 2),
                context.getString(R.string.astronomy_altitude_peak) to degrees(sun.altitude),
                context.getString(R.string.astronomy_direction_peak) to direction(peakAzimuth)
            )

            showAdvancedData(context.getString(R.string.solar_eclipse), advancedData)
        }
    }


}
