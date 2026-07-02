package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapImportRequest

interface ICreateMapCommand {
    suspend fun execute(): OfflineMapImportRequest?
}
