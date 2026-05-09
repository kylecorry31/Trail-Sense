package com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapPickers
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.OfflineMapFileService

class MoveOfflineMapFileCommand(
    private val context: Context
) : CoroutineCommand<IMap> {
    private val service = getAppService<OfflineMapFileService>()

    override suspend fun execute(value: IMap) {
        val results = MapPickers.pickGroup(
            context,
            null,
            context.getString(R.string.move),
            initialGroup = value.parentId
        ) {
            it.filter { group ->
                if (value is MapGroup) {
                    group.id != value.id
                } else {
                    true
                }
            }
        }

        if (results.first) {
            return
        }

        if (value is MapGroup) {
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
