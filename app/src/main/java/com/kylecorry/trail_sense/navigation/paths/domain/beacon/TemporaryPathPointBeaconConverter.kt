package com.kylecorry.trail_sense.navigation.paths.domain.beacon

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

class TemporaryPathPointBeaconConverter(private val defaultName: String) :
    IPathPointBeaconConverter {

    override fun toBeacon(path: Path, point: PathPoint): Beacon {
        return Beacon(
            0L,
            path.name ?: defaultName,
            point.coordinate,
            visible = false,
            elevation = point.elevation,
            temporary = true,
            color = path.style.color,
            owner = BeaconOwner.Path
        )
    }

}