package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

class CreateMapFromCameraCommand(
    private val fragment: AndromedaFragment,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): OfflineMapCatalogItem? = onIO {
        val uri = CustomUiUtils.takePhoto(fragment) ?: return@onIO null
        onMain {
            loadingIndicator.show()
        }
        try {
            val map = CreateMapFromUriCommand(
                fragment.requireContext(),
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
