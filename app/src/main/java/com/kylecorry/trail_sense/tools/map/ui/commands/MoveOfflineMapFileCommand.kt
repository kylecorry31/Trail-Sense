package com.kylecorry.trail_sense.tools.map.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.map.domain.IOfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.map.domain.OfflineMapFileGroup
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFilePickers
import com.kylecorry.trail_sense.tools.map.infrastructure.OfflineMapFileService

class MoveOfflineMapFileCommand(
    private val context: Context
) : CoroutineCommand<IOfflineMapFile> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: IOfflineMapFile) {
        val results = OfflineMapFilePickers.pickGroup(
            context,
            null,
            context.getString(R.string.move),
            initialGroup = value.parentId
        ) {
            it.filter { group ->
                if (value is OfflineMapFileGroup) {
                    group.id != value.id
                } else {
                    true
                }
            }
        }

        if (results.first) {
            return
        }

        if (value is OfflineMapFileGroup) {
            service.add(value.copy(parentId = results.second?.id))
        } else if (value is OfflineMapFile) {
            service.add(value.copy(parentId = results.second?.id))
        }

        val groupName = results.second?.name ?: context.getString(R.string.no_group)

        onMain {
            Alerts.toast(
                context,
                context.getString(R.string.moved_to, groupName)
            )
        }
    }
}
