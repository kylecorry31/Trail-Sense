package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.uri.GeoUri


class CreateBeaconFromPointCommand(private val context: Context) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        AppUtils.placeBeacon(
            context,
            GeoUri(point.coordinate, null, mapOf("label" to (path.name ?: context.getString(R.string.waypoint))))
        )
    }
}