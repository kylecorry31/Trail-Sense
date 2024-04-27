package com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.mappers

import com.kylecorry.trail_sense.shared.grouping.mapping.ISuspendMapper
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon

class BeaconNameMapper : ISuspendMapper<IBeacon, String> {
    override suspend fun map(item: IBeacon): String {
        return item.name
    }
}