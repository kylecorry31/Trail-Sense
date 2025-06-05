package com.kylecorry.trail_sense.tools.maps.ui.commands

import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.maps.domain.IMap
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapService

class ToggleVisibilityMapCommand(
    private val mapService: MapService
) : CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        if (value is PhotoMap) {
            mapService.add(value.copy(visible = !value.visible))
        }
    }
}