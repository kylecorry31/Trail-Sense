package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import com.kylecorry.trail_sense.shared.andromeda_temp.Result
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest

interface ICreateMapCommand {
    suspend fun execute(): Result<CreateOfflineMapRequest, CreateOfflineMapError>
}
