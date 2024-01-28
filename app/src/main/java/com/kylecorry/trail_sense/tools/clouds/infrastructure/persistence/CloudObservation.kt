package com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.data.Identifiable

data class CloudObservation(override val id: Long, val genus: CloudGenus?) : Identifiable