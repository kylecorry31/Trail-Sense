package com.kylecorry.trail_sense.tools.tides.domain.loading

import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideEntity

interface ITideLoader {
    suspend fun getReferenceTide(): TideEntity?
}