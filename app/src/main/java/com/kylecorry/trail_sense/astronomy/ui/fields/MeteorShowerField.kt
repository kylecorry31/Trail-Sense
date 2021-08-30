package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.astronomy.MeteorShowerPeak
import java.time.LocalDate

class MeteorShowerField(val date: LocalDate, val shower: MeteorShowerPeak) : AstroFieldTemplate() {
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

    private fun getMeteorShowerTime(
        context: Context,
        today: LocalDate,
        meteorShower: MeteorShowerPeak
    ): String {
        val formatService = FormatService(context)
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