package com.kylecorry.trail_sense.navigation.paths.domain.beacon

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

interface IPathPointBeaconConverter {
    fun toBeacon(path: Path, point: PathPoint): Beacon
}