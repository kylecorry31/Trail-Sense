package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.Eclipse
import com.kylecorry.trail_sense.shared.FormatService

class LunarEclipseField(
    private val eclipse: Eclipse,
    private val showStart: Boolean,
    private val altitude: Float
) :
    AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return if (showStart) {
            context.getString(R.string.lunar_eclipse_start)
        } else {
            context.getString(
                R.string.lunar_eclipse_end
            )
        }
    }

    override fun getValue(context: Context): String {
        return if (showStart) {
            getStartValue(context, eclipse)
        } else {
            getEndValue(context, eclipse)
        }
    }

    override fun getImage(context: Context): Int {
        return if (eclipse.isTotal) {
            R.drawable.ic_moon_total_eclipse
        } else {
            R.drawable.ic_moon_partial_eclipse
        }
    }

    override fun onClick(context: Context) {
        val formatService = FormatService.getInstance(context)
        val markdownService = MarkdownService(context)
        val text = context.getString(
            R.string.astro_dialog_eclipse,
            formatService.formatDateTime(eclipse.start, true),
            formatService.formatDateTime(eclipse.peak, true),
            formatService.formatDateTime(eclipse.end, true),
            formatService.formatDuration(eclipse.duration, short = false),
            if (eclipse.isTotal) context.getString(R.string.total) else context.getString(
                R.string.partial,
                formatService.formatPercentage(eclipse.magnitude * 100)
            ),
            formatService.formatDegrees(altitude)
        )

        Alerts.dialog(
            context,
            context.getString(R.string.lunar_eclipse),
            markdownService.toMarkdown(text),
            cancelText = null
        )
    }


    private fun getStartValue(context: Context, eclipse: Eclipse): String {
        val formatService = FormatService.getInstance(context)
        val eclipseAmount =
            if (eclipse.isTotal) context.getString(R.string.total) else context.getString(
                R.string.partial,
                formatService.formatPercentage(eclipse.magnitude * 100)
            )
        val time = formatService.formatTime(eclipse.start.toLocalTime(), includeSeconds = false)
        return "$time\n$eclipseAmount"
    }

    private fun getEndValue(context: Context, eclipse: Eclipse): String {
        val formatService = FormatService.getInstance(context)
        val eclipseAmount =
            if (eclipse.isTotal) context.getString(R.string.total) else context.getString(
                R.string.partial,
                formatService.formatPercentage(eclipse.magnitude * 100)
            )
        val time = formatService.formatTime(eclipse.end.toLocalTime(), includeSeconds = false)
        return "$time\n$eclipseAmount"
    }
}