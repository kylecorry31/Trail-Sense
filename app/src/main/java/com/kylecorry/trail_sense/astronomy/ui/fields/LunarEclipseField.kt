package com.kylecorry.trail_sense.astronomy.ui.fields

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.Eclipse
import com.kylecorry.trail_sense.astronomy.ui.format.EclipseFormatter
import com.kylecorry.trail_sense.shared.FormatService

class LunarEclipseField(
    private val eclipse: Eclipse,
    private val showStart: Boolean
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
        Alerts.dialog(
            context,
            context.getString(R.string.lunar_eclipse),
            EclipseFormatter.details(context, eclipse),
            cancelText = null
        )
    }

    private fun getStartValue(context: Context, eclipse: Eclipse): String {
        val formatService = FormatService.getInstance(context)
        val eclipseAmount = EclipseFormatter.type(context, eclipse)
        val time = formatService.formatTime(eclipse.start.toLocalTime(), includeSeconds = false)
        return "$time\n$eclipseAmount"
    }

    private fun getEndValue(context: Context, eclipse: Eclipse): String {
        val formatService = FormatService.getInstance(context)
        val eclipseAmount = EclipseFormatter.type(context, eclipse)
        val time = formatService.formatTime(eclipse.end.toLocalTime(), includeSeconds = false)
        return "$time\n$eclipseAmount"
    }
}