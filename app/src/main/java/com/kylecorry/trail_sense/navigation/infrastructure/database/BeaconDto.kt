package com.kylecorry.trail_sense.navigation.infrastructure.database

import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.Dto
import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType

class BeaconDto : Dto<Beacon>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            Pair("_id", SqlType.Long),
            Pair("name", SqlType.String),
            Pair("lat", SqlType.Double),
            Pair("lng", SqlType.Double),
            Pair("visible", SqlType.Boolean),
            Pair("comment", SqlType.NullableString),
            Pair("beacon_group_id", SqlType.NullableLong),
            Pair("elevation", SqlType.NullableFloat)
        )
    }

    override fun toObject(): Beacon {
        return Beacon(
            finalProperties["_id"] as Long,
            finalProperties["name"] as String,
            Coordinate(
                finalProperties["lat"] as Double,
                finalProperties["lng"] as Double
            ),
            finalProperties["visible"] as Boolean,
            finalProperties["comment"] as String?,
            finalProperties["beacon_group_id"] as Long?,
            finalProperties["elevation"] as Float?
        )
    }
}