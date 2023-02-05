package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo

class CreateMapFromCameraCommand(
    private val fragment: AndromedaFragment,
    private val repo: IMapRepo,
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