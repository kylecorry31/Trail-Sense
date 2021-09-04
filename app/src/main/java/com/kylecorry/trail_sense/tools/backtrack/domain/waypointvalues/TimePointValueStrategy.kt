package com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Duration
import java.time.Instant

class TimePointValueStrategy(private val context: Context) : IPointValueStrategy {
    override fun getValue(point: PathPoint): String {
        val time = point.time ?: return ""
        val duration = FormatService(context).formatDuration(Duration.between(time, Instant.now()))
        return context.getString(R.string.time_ago, duration)
    }
}