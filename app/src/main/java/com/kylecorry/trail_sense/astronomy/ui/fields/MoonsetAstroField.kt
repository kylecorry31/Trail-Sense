package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalTime

class MoonsetAstroField(val time: LocalTime) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.moon_set)
    }

    override fun getValue(context: Context): String {
        return FormatService.getInstance(context).formatTime(time, includeSeconds = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_moon_set
    }

    override fun onClick(context: Context) {
        val formatService = FormatService.getInstance(context)
        val markdownService = MarkdownService(context)
        val text = context.getString(
            R.string.astro_dialog_rise_set,
            formatService.formatTime(time, false),
            formatService.formatDirection(CompassDirection.West)
        )

        Alerts.dialog(
            context,
            getTitle(context),
            markdownService.toMarkdown(text),
            cancelText = null
        )
    }
}