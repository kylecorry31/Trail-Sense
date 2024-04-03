package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.readableName
import java.time.LocalDate

class MeteorShowerListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): ListItem? = onDefault {
        val shower = astronomyService.getMeteorShower(location, date) ?: return@onDefault null

        // Advanced
        val peakAltitude = astronomyService.getMeteorShowerPeakAltitude(shower, location)
        val peakAzimuth = DeclinationUtils.fromTrueNorthBearing(
            astronomyService.getMeteorShowerPeakAzimuth(shower, location),
            declination
        )

        list(
            3,
            context.getString(R.string.meteor_shower),
            context.getString(R.string.meteors_per_hour, shower.shower.rate),
            ResourceListIcon(R.drawable.ic_meteor, secondaryColor),
            data = times(shower.start, shower.peak, shower.end, date)
        ) {

            val advancedData = listOf(
                context.getString(R.string.times) to times(
                    shower.start,
                    shower.peak,
                    shower.end,
                    date
                ),
                context.getString(R.string.name) to data(shower.shower.readableName()),
                context.getString(R.string.rate) to data(
                    context.getString(R.string.meteors_per_hour, shower.shower.rate)
                ),
                context.getString(R.string.astronomy_altitude_peak) to degrees(peakAltitude),
                context.getString(R.string.astronomy_direction_peak) to direction(peakAzimuth)
            )

            showAdvancedData(context.getString(R.string.meteor_shower), advancedData)
        }
    }


}