package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import com.kylecorry.trail_sense.tools.maps.domain.Map

interface ICreateMapCommand {
    suspend fun execute(): Map?
}