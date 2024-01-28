package com.kylecorry.trail_sense.tools.paths.ui.commands

import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter

class NavigateToPointCommand(
    private val lifecycleOwner: LifecycleOwner,
    private val converter: IPathPointBeaconConverter,
    private val beaconNavigator: IBeaconNavigator
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        lifecycleOwner.inBackground {
            val beacon = converter.toBeacon(path, point)
            beaconNavigator.navigateTo(beacon)
        }
    }
}