package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.IOfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapFileService

class DeleteOfflineMapCommand(
    private val context: Context
) : CoroutineCommand<IOfflineMapFile> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: IOfflineMapFile) {
        val shouldDelete = onMain {
            !CoroutineAlerts.dialog(
                context,
                context.getString(R.string.delete),
                if (value.isGroup) {
                    context.getString(R.string.delete_offline_map_group_message, value.name)
                } else {
                    value.name
                }
            )
        }

        if (!shouldDelete) {
            return
        }
        service.delete(value)
    }
}
