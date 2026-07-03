package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest

class CreateMapFromCameraCommand(
    private val fragment: AndromedaFragment
) : ICreateMapCommand {
    override suspend fun execute(): CreateOfflineMapRequest? = onIO {
        val uri = CustomUiUtils.takePhoto(fragment) ?: return@onIO null
        CreateMapFromUriCommand(
            fragment.requireContext(),
            uri
        ).execute()
    }
}
