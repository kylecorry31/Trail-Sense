package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo

class CreateMapFromFileCommand(
    private val context: Context,
    private val uriPicker: UriPicker,
    private val repo: IMapRepo,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): Map? = onIO {
        val uri = uriPicker.open(listOf("image/*", "application/pdf")) ?: return@onIO null
        CreateMapFromUriCommand(context, repo, uri, loadingIndicator).execute()
    }
}