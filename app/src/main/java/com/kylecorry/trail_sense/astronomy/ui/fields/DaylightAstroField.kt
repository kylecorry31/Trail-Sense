package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

class DaylightAstroField(val length: Duration, val season: Season) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.daylight)
    }

    override fun getValue(context: Context): String {
        return FormatService.getInstance(context).formatDuration(length, short = false)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_sun
    }

    override fun onClick(context: Context) {
        val formatService = FormatService.getInstance(context)
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