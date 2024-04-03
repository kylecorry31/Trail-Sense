package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import java.time.Duration
import java.time.LocalDate

class SunListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): ListItem = onDefault {
        // At a glance
        val times = astronomyService.getSunTimes(location, prefs.astronomy.sunTimesMode, date)
        val daylight = astronomyService.getLengthOfDay(location, prefs.astronomy.sunTimesMode, date)

        // Advanced
        val actual = astronomyService.getSunTimes(location, SunTimesMode.Actual, date)
        val civil = astronomyService.getSunTimes(location, SunTimesMode.Civil, date)
        val nautical = astronomyService.getSunTimes(location, SunTimesMode.Nautical, date)
        val astronomical = astronomyService.getSunTimes(location, SunTimesMode.Astronomical, date)
        val peak = times.transit?.let {
            astronomyService.getSunAltitude(location, it)
        }
        val night = Duration.ofDays(1) - daylight
        val season = astronomyService.getSeason(location, date)
        val azimuth = if (date == LocalDate.now()) DeclinationUtils.fromTrueNorthBearing(
            astronomyService.getSunAzimuth(location),
            declination
        ) else null
        val altitude =
            if (date == LocalDate.now()) astronomyService.getSunAltitude(location) else null

        list(
            1,
            context.getString(R.string.sun),
            context.getString(
                R.string.daylight_duration,
                formatter.formatDuration(daylight, false)
            ),
            ResourceListIcon(R.drawable.ic_sun),
            data = riseSetTransit(times)
        ) {
            val advancedData = listOf(
                context.getString(R.string.sun_actual) to riseSetTransit(actual),
                context.getString(R.string.sun_civil) to riseSetTransit(civil),
                context.getString(R.string.sun_nautical) to riseSetTransit(nautical),
                context.getString(R.string.sun_astronomical) to riseSetTransit(astronomical),
                context.getString(R.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                context.getString(R.string.daylight) to duration(daylight),
                context.getString(R.string.night) to duration(night),
                context.getString(R.string.season) to data(formatter.formatSeason(season)),
                context.getString(R.string.astronomy_altitude) to altitude?.let { degrees(it) },
                context.getString(R.string.direction) to azimuth?.let { degrees(it.value) }
            )

            showAdvancedData(context.getString(R.string.sun), advancedData)
        }
    }


}