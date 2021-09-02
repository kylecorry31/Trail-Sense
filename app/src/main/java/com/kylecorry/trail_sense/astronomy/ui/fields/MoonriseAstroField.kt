package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.units.CompassDirection
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalTime

class MoonriseAstroField(val time: LocalTime) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.moon_rise)
    }

    override fun getValue(context: Context): String {
        return FormatService(context).formatTime(time, includeSeconds = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_moon_rise
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