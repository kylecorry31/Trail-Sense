package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

interface ICreateMapCommand {
    suspend fun execute(): IMap?
}
