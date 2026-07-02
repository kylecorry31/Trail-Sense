package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.MapService

class ToggleVisibilityMapCommand(
    private val mapService: MapService
) : CoroutineCommand<OfflineMapCatalogItem> {
    override suspend fun execute(value: OfflineMapCatalogItem) {
        when (value) {
            is PhotoMap -> mapService.add(value.copy(visible = !value.visible))
            is TrailMap -> mapService.add(value.copy(visible = !value.visible))
        }
    }
}
