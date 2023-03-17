package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

class DaylightSavingsField(val change: Duration) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return "Daylight savings"
    }

    override fun getValue(context: Context): String {
        val formatter = FormatService.getInstance(context)

        if (change.isNegative) {
            return "- " + formatter.formatDuration(change.negated(), short = false)
        }

        return "+ " + formatter.formatDuration(change, short = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_tool_clock
    }
}