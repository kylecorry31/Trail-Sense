package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.point_finder.IPathPointNavigator

class NavigateToPathCommand(
    private val navigator: IPathPointNavigator,
    private val gps: IGPS,
    private val converter: IPathPointBeaconConverter,
    private val beaconNavigator: IBeaconNavigator
) {
    suspend fun execute(path: Path, points: List<PathPoint>) {
        val point = navigator.getNextPoint(points, gps.location) ?: return
        val beacon = converter.toBeacon(path, point)
        beaconNavigator.navigateTo(beacon)
    }
}