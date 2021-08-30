package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

class DaylightAstroField(val length: Duration) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.daylight)
    }

    override fun getValue(context: Context): String {
        return FormatService(context).formatDuration(length, short = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_sun
    }
}