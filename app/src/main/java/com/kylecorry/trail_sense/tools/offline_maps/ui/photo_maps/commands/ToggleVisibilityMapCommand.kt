package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.commands

import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapService

class ToggleVisibilityMapCommand(
    private val mapService: MapService
) : CoroutineCommand<IMap> {
    override suspend fun execute(value: IMap) {
        if (value is PhotoMap) {
            mapService.add(value.copy(visible = !value.visible))
        }
    }
}
