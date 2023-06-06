package com.kylecorry.trail_sense.astronomy.ui.items

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
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
        ) {
            Alerts.dialog(
                context,
                context.getString(R.string.sun),
                fields(
                    // Actual
                    context.getString(R.string.sun_actual) to riseSet(
                        actual.rise,
                        actual.set
                    ),

                    // Civil
                    context.getString(R.string.sun_civil) to riseSet(
                        civil.rise,
                        civil.set
                    ),

                    // Nautical
                    context.getString(R.string.sun_nautical) to riseSet(
                        nautical.rise,
                        nautical.set
                    ),

                    // Astronomical
                    context.getString(R.string.sun_astronomical) to riseSet(
                        astronomical.rise,
                        astronomical.set
                    ),

                    // Peak
                    context.getString(R.string.noon) to time(actual.transit),
                    context.getString(R.string.astronomy_altitude_peak) to peak?.let {
                        formatter.formatDegrees(
                            it
                        )
                    },

                    // Light
                    context.getString(R.string.daylight) to formatter.formatDuration(
                        daylight,
                        false
                    ),
                    context.getString(R.string.night) to formatter.formatDuration(
                        night,
                        false
                    ),
                    context.getString(R.string.season) to formatter.formatSeason(season)
                ),
                cancelText = null
            )
        }
    }


}