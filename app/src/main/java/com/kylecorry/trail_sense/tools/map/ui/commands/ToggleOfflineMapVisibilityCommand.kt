package com.kylecorry.trail_sense.tools.map.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.infrastructure.persistence.OfflineMapFileRepo

class ToggleOfflineMapVisibilityCommand : CoroutineCommand<OfflineMapFile> {
    private val repo = getAppService<OfflineMapFileRepo>()

    override suspend fun execute(value: OfflineMapFile) {
        repo.add(value.copy(visible = !value.visible))
    }
}
