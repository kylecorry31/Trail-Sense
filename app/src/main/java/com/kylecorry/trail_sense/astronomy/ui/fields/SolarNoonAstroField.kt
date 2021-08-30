package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalTime

class SolarNoonAstroField(val time: LocalTime) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.solar_noon)
    }

    override fun getValue(context: Context): String {
        return FormatService(context).formatTime(time, includeSeconds = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_sun
    }
}