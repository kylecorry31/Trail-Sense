package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileService

class ToggleOfflineMapVisibilityCommand : CoroutineCommand<IMap> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: IMap) {
        if (value is OfflineMapFile) {
            service.add(value.copy(visible = !value.visible))
        }
    }
}
