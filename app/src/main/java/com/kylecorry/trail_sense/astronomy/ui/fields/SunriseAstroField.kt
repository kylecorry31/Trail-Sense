package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalTime

class SunriseAstroField(val time: LocalTime, val type: SunTimesMode) :
    AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return when (type) {
            SunTimesMode.Actual -> context.getString(R.string.sunrise_label)
            SunTimesMode.Civil -> context.getString(
                R.string.dawn_type,
                context.getString(R.string.sun_civil)
            )
            SunTimesMode.Nautical -> context.getString(
                R.string.dawn_type,
                context.getString(R.string.sun_nautical)
            )
            SunTimesMode.Astronomical -> context.getString(
                R.string.dawn_type,
                context.getString(R.string.sun_astronomical)
            )
        }
    }

    override fun getValue(context: Context): String {
        return FormatService(context).formatTime(time, includeSeconds = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_sun_rise
    }

    override fun onClick(context: Context) {
        val formatService = FormatService(context)
        val markdownService = MarkdownService(context)
        val text = context.getString(
            R.string.astro_dialog_rise_set,
            formatService.formatTime(time, false),
            formatService.formatDirection(CompassDirection.East)
        )

        Alerts.dialog(
            context,
            getTitle(context),
            markdownService.toMarkdown(text),
            cancelText = null
        )
    }
}