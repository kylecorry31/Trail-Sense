package com.kylecorry.trail_sense.navigation.infrastructure.export

import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup

data class BeaconExport(val beacons: List<Beacon>, val groups: List<BeaconGroup>)