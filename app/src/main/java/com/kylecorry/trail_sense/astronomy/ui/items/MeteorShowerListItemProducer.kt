package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import java.time.LocalDate

class MeteorShowerListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
    ): ListItem? = onDefault {
        val shower = astronomyService.getMeteorShower(location, date) ?: return@onDefault null

        // Advanced
        val peakAltitude = astronomyService.getMeteorShowerPeakAltitude(shower, location)
        val peakAzimuth = astronomyService.getMeteorShowerPeakAzimuth(shower, location)

        listItem(
            3,
            context.getString(R.string.meteor_shower),
            context.getString(R.string.meteors_per_hour, shower.shower.rate),
            ResourceListIcon(R.drawable.ic_meteor, secondaryColor),
            data = listOf(timeData(shower.peak, date, context.getString(R.string.peak_time)))
        ) {
            Alerts.dialog(
                context,
                context.getString(R.string.meteor_shower),
                fields(
                    // Name
                    context.getString(R.string.name) to shower.shower.name.map { if (it.isUpperCase()) " $it" else it }
                        .joinToString("").trim(),

                    // Rate
                    context.getString(R.string.rate) to context.getString(
                        R.string.meteors_per_hour,
                        shower.shower.rate
                    ),

                    // Peak
                    context.getString(R.string.peak_time) to time(shower.peak, date),
                    context.getString(R.string.astronomy_altitude_peak) to formatter.formatDegrees(
                        peakAltitude
                    ),
                    context.getString(R.string.astronomy_direction_peak) to formatter.formatDirection(
                        peakAzimuth.direction
                    )

                ),
                cancelText = null
            )
        }
    }


}