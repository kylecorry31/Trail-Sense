package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalDate

class MeteorShowerField(
    private val date: LocalDate,
    private val shower: MeteorShowerPeak,
    private val azimuth: Bearing,
    private val altitude: Float
) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.meteor_shower)
    }

    override fun getValue(context: Context): String {
        return getMeteorShowerTime(context, date, shower)
    }

    override fun getImage(context: Context): Int {
        return R.drawable.ic_meteor
    }

    override fun getImageTint(context: Context): Int {
        return Resources.androidTextColorSecondary(context)
    }

    override fun onClick(context: Context) {
        val formatService = FormatService.getInstance(context)
        val markdownService = MarkdownService(context)
        val text = context.getString(
            R.string.astro_dialog_meteor_shower,
            formatService.formatDateTime(shower.peak, true),
            formatService.formatDegrees(altitude),
            formatService.formatDirection(azimuth.direction),
            context.getString(R.string.meteors_per_hour, shower.shower.rate)
        )

        Alerts.dialog(
            context,
            shower.shower.name.map { if (it.isUpperCase()) " $it" else it }.joinToString("").trim(),
            markdownService.toMarkdown(text),
            cancelText = null
        )
    }

    private fun getMeteorShowerTime(
        context: Context,
        today: LocalDate,
        meteorShower: MeteorShowerPeak
    ): String {
        val formatService = FormatService.getInstance(context)
        return if (meteorShower.peak.toLocalDate() == today) {
            formatService.formatTime(meteorShower.peak.toLocalTime(), false)
        } else {
            context.getString(
                R.string.tomorrow_at,
                formatService.formatTime(meteorShower.peak.toLocalTime(), false)
            )
        } + "\n${context.getString(R.string.meteors_per_hour, meteorShower.shower.rate)}"
    }
}