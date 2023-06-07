package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.ceres.list.ListItem
import com.kylecorry.ceres.list.ResourceListIcon
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import java.time.Duration
import java.time.LocalDate

class SunListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate
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

        list(
            1,
            context.getString(R.string.sun),
            context.getString(
                R.string.daylight_duration,
                formatter.formatDuration(daylight, false)
            ),
            ResourceListIcon(R.drawable.circle, AppColor.Yellow.color),
            data = riseSet(times.rise, times.set)
        ) {
            val advancedData = listOf(
                context.getString(R.string.sun_actual) to riseSet(actual.rise, actual.set),
                context.getString(R.string.sun_civil) to riseSet(civil.rise, civil.set),
                context.getString(R.string.sun_nautical) to riseSet(nautical.rise, nautical.set),
                context.getString(R.string.sun_astronomical) to riseSet(astronomical.rise, astronomical.set),
                context.getString(R.string.noon) to time(actual.transit),
                context.getString(R.string.astronomy_altitude_peak) to peak?.let { degrees(it) },
                context.getString(R.string.daylight) to duration(daylight),
                context.getString(R.string.night) to duration(night),
                context.getString(R.string.season) to data(formatter.formatSeason(season))
            ).filter { it.second != null }

            showAdvancedData(context.getString(R.string.sun), advancedData)
        }
    }


}