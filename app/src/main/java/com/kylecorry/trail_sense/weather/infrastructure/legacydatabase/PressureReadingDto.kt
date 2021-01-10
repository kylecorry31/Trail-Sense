package com.kylecorry.trail_sense.weather.infrastructure.legacydatabase

import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.persistence.Dto
import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType
import java.time.Instant

class PressureReadingDto : Dto<PressureAltitudeReading>() {
    override fun getProperties(): Map<String, SqlType> {
        return mapOf(
            "_id" to SqlType.Int,
            "time" to SqlType.Long,
            "pressure" to SqlType.Float,
            "altitude" to SqlType.Float,
            "altitude_accuracy" to SqlType.NullableFloat,
            "temperature" to SqlType.Float
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