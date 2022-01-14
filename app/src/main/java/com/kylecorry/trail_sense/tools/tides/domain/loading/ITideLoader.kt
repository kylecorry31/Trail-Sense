package com.kylecorry.trail_sense.tools.tides.domain.loading

import com.kylecorry.trail_sense.tools.tides.domain.TideTable

interface ITideLoader {
    suspend fun getTideTable(): TideTable?
}