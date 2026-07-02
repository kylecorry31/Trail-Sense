package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

interface ICreateMapCommand {
    suspend fun execute(): OfflineMapCatalogItem?
}
