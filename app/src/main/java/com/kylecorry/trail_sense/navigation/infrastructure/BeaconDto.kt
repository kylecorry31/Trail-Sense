package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.shared.Dto
import com.kylecorry.trail_sense.shared.SqlType
import com.kylecorry.trail_sense.shared.domain.Coordinate

class BeaconDto : Dto<Beacon>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            Pair("_id", SqlType.Int),
            Pair("name", SqlType.String),
            Pair("lat", SqlType.Double),
            Pair("lng", SqlType.Double),
            Pair("visible", SqlType.Boolean)
        )
    }

    override fun toObject(): Beacon {
        return Beacon(
            finalProperties["_id"] as Int,
            finalProperties["name"] as String,
            Coordinate(
                finalProperties["lat"] as Double,
                finalProperties["lng"] as Double
            ),
            finalProperties["visible"] as Boolean
        )
    }
}