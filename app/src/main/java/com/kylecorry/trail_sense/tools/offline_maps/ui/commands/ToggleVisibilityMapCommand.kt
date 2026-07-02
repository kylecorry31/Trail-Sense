package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService

class ToggleVisibilityMapCommand(
    private val mapService: OfflineMapService
) : CoroutineCommand<OfflineMapCatalogItem> {
    override suspend fun execute(value: OfflineMapCatalogItem) {
        if (value is OfflineMap) {
            mapService.setVisible(value, !value.visible)
        }
    }
}
