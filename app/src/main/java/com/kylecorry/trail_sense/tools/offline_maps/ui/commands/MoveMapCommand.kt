package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.ui.MapPickers
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService

class MoveMapCommand(private val context: Context, private val service: OfflineMapService) :
    CoroutineCommand<OfflineMapCatalogItem> {
    override suspend fun execute(value: OfflineMapCatalogItem) {
        val results = MapPickers.pickGroup(
            context,
            null,
            context.getString(R.string.move),
            initialGroup = value.parentId
        ) {
            it.filter {
                if (value is MapGroup) {
                    it.id != value.id
                } else {
                    true
                }
            }
        }

        if (results.first) {
            return
        }

        service.move(value, results.second?.id)

        val groupName = results.second?.name ?: context.getString(R.string.no_group)

        onMain {
            Alerts.toast(
                context,
                context.getString(R.string.moved_to, groupName)
            )
        }
    }
}
