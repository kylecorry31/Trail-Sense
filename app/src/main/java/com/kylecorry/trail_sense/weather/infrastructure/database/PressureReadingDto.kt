package com.kylecorry.trail_sense.weather.infrastructure.database

import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.Dto
import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType
import java.time.Instant

class PressureReadingDto : Dto<PressureAltitudeReading>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            Pair("_id", SqlType.Int),
            Pair("time", SqlType.Long),
            Pair("pressure", SqlType.Float),
            Pair("altitude", SqlType.Float),
            Pair("altitude_accuracy", SqlType.NullableFloat),
            Pair("temperature", SqlType.Float)
        )
    }

    override fun toObject(): PressureAltitudeReading {
        return PressureAltitudeReading(
            Instant.ofEpochMilli(finalProperties["time"] as Long),
            finalProperties["pressure"] as Float,
            finalProperties["altitude"] as Float,
            finalProperties["temperature"] as Float
        )
    }
}