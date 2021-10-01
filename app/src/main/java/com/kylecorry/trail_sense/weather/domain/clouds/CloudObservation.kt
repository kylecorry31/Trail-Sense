package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.trail_sense.shared.database.Identifiable

data class CloudObservation(override val id: Long, val coverage: Float) : Identifiable