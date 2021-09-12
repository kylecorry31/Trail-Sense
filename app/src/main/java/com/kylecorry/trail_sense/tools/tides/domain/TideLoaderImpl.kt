package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TideLoaderImpl(
    private val tideRepo: ITideRepo,
    private val strategy: ITideSelectionStrategy
) : ITideLoader {
    override suspend fun getReferenceTide(): TideEntity? = withContext(Dispatchers.IO) {
        val tides = tideRepo.getTidesSuspend()
        strategy.getTide(tides)
    }
}