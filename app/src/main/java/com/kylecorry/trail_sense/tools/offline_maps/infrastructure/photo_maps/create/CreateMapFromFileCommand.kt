package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.create

import android.content.Context
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.IMapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.MapRepo

class CreateMapFromFileCommand(
    private val context: Context,
    private val uriPicker: UriPicker,
    private val repo: MapRepo,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): PhotoMap? = onIO {
        val uri = uriPicker.open(listOf("image/*", "application/pdf")) ?: return@onIO null
        CreateMapFromUriCommand(context, repo, uri, loadingIndicator).execute()
    }
}
