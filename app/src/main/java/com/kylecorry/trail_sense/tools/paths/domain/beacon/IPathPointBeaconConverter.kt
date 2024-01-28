package com.kylecorry.trail_sense.tools.paths.domain.beacon

import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

interface IPathPointBeaconConverter {
    fun toBeacon(path: Path, point: PathPoint): Beacon
}