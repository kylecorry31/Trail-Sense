package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences

class MoonPhaseAstroField(val phase: MoonPhase, val isSupermoon: Boolean) : AstroFieldTemplate() {
    override fun getTitle(context: Context): String {
        return context.getString(R.string.moon_phase)
    }

    override fun getValue(context: Context): String {
        val prefs = UserPreferences(context)
        val formatService = FormatService(context)
        val illuminationString =
            if (prefs.astronomy.showMoonIllumination) " (" + formatService.formatPercentage(
                phase.illumination
            ) + ")" else ""
        return formatService.formatMoonPhase(phase.phase) + illuminationString
    }

    override fun getImage(context: Context): Int {
        return MoonPhaseImageMapper().getPhaseImage(phase.phase)
    }

    override fun onClick(context: Context) {
        val formatService = FormatService(context)
        val markdownService = MarkdownService(context)
        val text = context.getString(
            R.string.astro_dialog_moon_phase,
            formatService.formatMoonPhase(phase.phase) + if (isSupermoon) " (${context.getString(R.string.supermoon)})" else "",
            formatService.formatPercentage(phase.illumination)
        )

        Alerts.dialog(
            context,
            getTitle(context),
            markdownService.toMarkdown(text),
            cancelText = null
        )
    }
}