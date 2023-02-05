package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap

interface ICreateMapCommand {
    suspend fun execute(): PhotoMap?
}