package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

interface ICreateMapCommand {
    suspend fun execute(): PhotoMap?
}
