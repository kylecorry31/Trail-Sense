package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

interface ICreateMapCommand {
    suspend fun execute(): PhotoMap?
}