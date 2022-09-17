package com.kylecorry.trail_sense.weather.infrastructure.persistence

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.database.Identifiable

data class CloudObservation(override val id: Long, val genus: CloudGenus?) : Identifiable