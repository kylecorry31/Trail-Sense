package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import android.content.Context
import com.kylecorry.andromeda.pickers.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService

class RenameMapCommand(private val context: Context, private val service: OfflineMapService) :
    CoroutineCommand<OfflineMapCatalogItem> {
    override suspend fun execute(value: OfflineMapCatalogItem) {
        val newName =
            CoroutinePickers.text(context, context.getString(R.string.name), default = value.name)
                ?: return
        service.rename(value, newName)
    }
}
