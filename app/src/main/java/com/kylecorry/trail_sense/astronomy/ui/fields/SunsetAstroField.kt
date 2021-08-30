package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.astronomy.SunTimesMode
import java.time.LocalTime

class SunsetAstroField(val time: LocalTime, val type: SunTimesMode) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return when (type) {
            SunTimesMode.Actual -> context.getString(R.string.sunset_label)
            SunTimesMode.Civil -> context.getString(
                R.string.dusk_type,
                context.getString(R.string.sun_civil)
            )
            SunTimesMode.Nautical -> context.getString(
                R.string.dusk_type,
                context.getString(R.string.sun_nautical)
            )
            SunTimesMode.Astronomical -> context.getString(
                R.string.dusk_type,
                context.getString(R.string.sun_astronomical)
            )
        }
    }

    override fun getValue(context: Context): String {
        return FormatService(context).formatTime(time, includeSeconds = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_sun_set
    }
}