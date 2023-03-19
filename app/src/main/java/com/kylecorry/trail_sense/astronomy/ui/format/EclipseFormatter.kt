package com.kylecorry.trail_sense.astronomy.ui.format

import android.content.Context
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.Eclipse
import com.kylecorry.trail_sense.shared.FormatService

object EclipseFormatter {

    fun details(context: Context, eclipse: Eclipse): CharSequence {
        val formatService = FormatService.getInstance(context)
        val markdownService = MarkdownService(context)
        val fields = listOf(
            formatEclipseField(
                context,
                R.string.start_time,
                formatService.formatDateTime(eclipse.start, relative = true, abbreviateMonth = true)
            ),
            formatEclipseField(
                context,
                R.string.peak_time,
                formatService.formatDateTime(eclipse.peak, relative = true, abbreviateMonth = true)
            ),
            formatEclipseField(
                context,
                R.string.end_time,
                formatService.formatDateTime(eclipse.end, relative = true, abbreviateMonth = true)
            ),
            formatEclipseField(
                context,
                R.string.duration,
                formatService.formatDuration(eclipse.duration, short = false)
            ),
            formatEclipseField(
                context,
                R.string.magnitude,
                DecimalFormatter.format(eclipse.magnitude, 2)
            ),
            formatEclipseField(
                context,
                R.string.obscuration,
                type(context, eclipse).toString()
            ),
            formatEclipseField(
                context,
                R.string.astronomy_altitude_peak,
                formatService.formatDegrees(eclipse.peakAltitude)
            ),
            formatEclipseField(
                context,
                R.string.astronomy_direction_peak,
                formatService.formatDirection(eclipse.peakDirection.direction)
            )
        )


        val text = fields.joinToString("\n\n")

        return markdownService.toMarkdown(text)
    }

    fun type(context: Context, eclipse: Eclipse): CharSequence {
        val formatService = FormatService.getInstance(context)
        return if (eclipse.isTotal) context.getString(R.string.total) else context.getString(
            R.string.partial,
            formatService.formatPercentage(eclipse.obscuration * 100)
        )
    }

    private fun formatEclipseField(context: Context, id: Int, value: String): String {
        val fieldName = context.getString(id)
        return "**$fieldName**: $value"
    }

}