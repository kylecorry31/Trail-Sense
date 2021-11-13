package com.kylecorry.trail_sense.tools.backtrack.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.paths.Path
import com.kylecorry.trail_sense.shared.paths.PathPoint


class CreateBeaconFromPointCommand(private val context: Context) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        AppUtils.placeBeacon(
            context,
            MyNamedCoordinate(point.coordinate, path.name ?: context.getString(R.string.waypoint))
        )
    }
}