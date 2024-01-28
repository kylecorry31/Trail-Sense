package com.kylecorry.trail_sense.tools.lightning.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.data.Identifiable

data class LightningStrike(override val id: Long, val distance: Distance) : Identifiable