package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.astronomy.moon.MoonPhase

class MoonPhaseAstroField(val phase: MoonPhase) : AstroFieldTemplate() {
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
}