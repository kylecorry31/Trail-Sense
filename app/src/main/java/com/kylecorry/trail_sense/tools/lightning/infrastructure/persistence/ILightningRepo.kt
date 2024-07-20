package com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence

import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike

interface ILightningRepo : IReadingRepo<LightningStrike> {
    suspend fun getLast(): Reading<LightningStrike>?
}