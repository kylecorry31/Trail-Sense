package com.kylecorry.trail_sense.navigation.paths.ui.commands

import androidx.lifecycle.LifecycleCoroutineScope
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.beacon.IPathPointBeaconConverter

class NavigateToPointCommand(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val converter: IPathPointBeaconConverter,
    private val beaconNavigator: IBeaconNavigator
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        lifecycleScope.launchWhenResumed {
            val beacon = converter.toBeacon(path, point)
            beaconNavigator.navigateTo(beacon)
        }
    }
}