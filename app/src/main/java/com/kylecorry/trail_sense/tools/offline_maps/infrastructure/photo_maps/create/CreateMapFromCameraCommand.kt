package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create

import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.IMapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapRepo

class CreateMapFromCameraCommand(
    private val fragment: AndromedaFragment,
    private val repo: MapRepo,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): PhotoMap? = onIO {
        val uri = CustomUiUtils.takePhoto(fragment) ?: return@onIO null
        onMain {
            loadingIndicator.show()
        }
        try {
            val map = CreateMapFromUriCommand(
                fragment.requireContext(),
                repo,
                uri,
                loadingIndicator
            ).execute()
            DeleteTempFilesCommand(fragment.requireContext()).execute()
            map
        } finally {
            onMain {
                loadingIndicator.hide()
            }
        }
    }
}
