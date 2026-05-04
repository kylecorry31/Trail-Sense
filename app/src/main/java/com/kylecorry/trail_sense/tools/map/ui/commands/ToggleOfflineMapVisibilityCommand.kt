package com.kylecorry.trail_sense.tools.map.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.IOfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFileService

class ToggleOfflineMapVisibilityCommand : CoroutineCommand<IOfflineMapFile> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: IOfflineMapFile) {
        if (value is OfflineMapFile) {
            service.add(value.copy(visible = !value.visible))
        }
    }
}
