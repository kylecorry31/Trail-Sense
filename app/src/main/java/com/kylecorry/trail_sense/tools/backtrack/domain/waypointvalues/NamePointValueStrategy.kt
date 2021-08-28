package com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues

import android.content.Context
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class NamePointValueStrategy(private val context: Context) : IPointValueStrategy {
    override fun getValue(point: PathPoint): String {
        if (point.time == null) {
            return context.getString(android.R.string.untitled)
        }
        val date = point.time!!.toZonedDateTime()
        val time = date.toLocalTime()
        val formatService = FormatService(context)
        return context.getString(
            R.string.waypoint_time_format,
            formatService.formatRelativeDate(date.toLocalDate()),
            formatService.formatTime(time, false)
        )
    }
}