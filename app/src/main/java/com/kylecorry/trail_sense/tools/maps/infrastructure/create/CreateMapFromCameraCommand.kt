package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo

class CreateMapFromCameraCommand(
    private val fragment: AndromedaFragment,
    private val repo: IMapRepo,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): Map? = onIO {
        val uri = CustomUiUtils.takePhoto(fragment) ?: return@onIO null
        onMain {
            loadingIndicator.show()
        }
        val map = CreateMapFromUriCommand(
            fragment.requireContext(),
            repo,
            uri,
            loadingIndicator
        ).execute()
        DeleteTempFilesCommand(fragment.requireContext()).execute()
        map
    }
}