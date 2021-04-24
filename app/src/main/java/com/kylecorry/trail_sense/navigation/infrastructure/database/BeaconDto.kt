package com.kylecorry.trail_sense.navigation.infrastructure.database

import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.Dto
import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType

class BeaconDto : Dto<Beacon>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            "_id" to SqlType.Long,
            "name" to SqlType.String,
            "lat" to SqlType.Double,
            "lng" to SqlType.Double,
            "visible" to SqlType.Boolean,
            "comment" to SqlType.NullableString,
            "beacon_group_id" to SqlType.NullableLong,
            "elevation" to SqlType.NullableFloat,
            "temporary" to SqlType.Boolean
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
            finalProperties["elevation"] as Float?,
            finalProperties["temporary"] as Boolean,
            color = AppColor.Orange.color
        )
    }
}