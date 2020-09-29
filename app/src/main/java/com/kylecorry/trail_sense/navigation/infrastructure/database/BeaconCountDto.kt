package com.kylecorry.trail_sense.navigation.infrastructure.database

import com.kylecorry.trailsensecore.infrastructure.persistence.Dto
import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType

class BeaconCountDto : Dto<Int>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            Pair("cnt", SqlType.Int)
        )
    }

    override fun toObject(): Int {
        return finalProperties["cnt"] as Int
    }
}