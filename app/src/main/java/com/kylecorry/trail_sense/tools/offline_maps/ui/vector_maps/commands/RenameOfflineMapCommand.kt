package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.IOfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFileGroup
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileService

class RenameOfflineMapCommand(
    private val context: Context
) : CoroutineCommand<IOfflineMapFile> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: IOfflineMapFile) {
        val name = CoroutinePickers.text(
            context,
            context.getString(R.string.name),
            hint = context.getString(R.string.name),
            default = value.name
        )?.trim()?.takeIf { it.isNotBlank() } ?: return
        if (value is OfflineMapFileGroup) {
            service.add(value.copy(name = name))
        } else if (value is OfflineMapFile) {
            service.add(value.copy(name = name))
        }
    }
}
