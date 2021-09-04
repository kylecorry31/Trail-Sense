package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.time.Season
import java.time.Duration

class DaylightAstroField(val length: Duration, val season: Season) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.daylight)
    }

    override fun getValue(context: Context): String {
        return FormatService(context).formatDuration(length, short = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_sun
    }

    override fun onClick(context: Context) {
        val formatService = FormatService(context)
        val markdownService = MarkdownService(context)
        val text = context.getString(
            R.string.astro_dialog_day_length,
            formatService.formatDuration(length, false),
            formatService.formatDuration(Duration.ofDays(1).minus(length), false),
            formatService.formatSeason(season)
        )

        Alerts.dialog(
            context,
            getTitle(context),
            markdownService.toMarkdown(text),
            cancelText = null
        )
    }
}