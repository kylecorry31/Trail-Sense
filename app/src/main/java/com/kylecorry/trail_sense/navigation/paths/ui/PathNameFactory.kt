package com.kylecorry.trail_sense.navigation.paths.ui

import android.content.Context
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.shared.FormatService

class PathNameFactory(private val context: Context) {

    private val formatService = FormatService.getInstance(context)

    @Suppress("IfThenToElvis")
    fun getName(path: Path): String {
        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end
        return if (path.name != null) {
            path.name
        } else if (start != null && end != null) {
            formatService.formatTimeSpan(start.toZonedDateTime(), end.toZonedDateTime(), true)
        } else {
            context.getString(android.R.string.untitled)
        }
    }

}