package com.kylecorry.trail_sense.weather.domain.lightning

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.database.Identifiable

data class LightningStrike(override val id: Long, val distance: Distance) : Identifiable