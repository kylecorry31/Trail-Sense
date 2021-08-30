package com.kylecorry.trail_sense.navigation.infrastructure.database

import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trail_sense.shared.database.Dto
import com.kylecorry.trail_sense.shared.database.SqlType

class BeaconGroupDto : Dto<BeaconGroup>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            "beacon_group_id" to SqlType.Long,
            "group_name" to SqlType.String
        )
    }

    override fun toObject(): BeaconGroup {
        return BeaconGroup(
            finalProperties["beacon_group_id"] as Long,
            finalProperties["group_name"] as String
        )
    }
}