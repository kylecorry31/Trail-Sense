package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService

class DeleteMapCommand(
    private val context: Context,
    private val mapService: OfflineMapService
) {
    suspend fun execute(value: OfflineMapCatalogItem): Boolean {
        val shouldDelete = onMain {
            !CoroutineAlerts.dialog(
                context,
                context.getString(R.string.delete),
                if (!value.isGroup) value.name else context.getString(
                    R.string.delete_map_group_message,
                    value.name
                )
            )
        }

        if (!shouldDelete) {
            return false
        }

        mapService.delete(value)
        return true
    }
}
