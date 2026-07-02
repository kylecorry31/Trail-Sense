package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import android.content.Context
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapCatalogItem

class CreateMapFromFileCommand(
    private val context: Context,
    private val uriPicker: UriPicker,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {
    override suspend fun execute(): OfflineMapCatalogItem? = onIO {
        val uri = uriPicker.open(
            listOf("image/*", "application/pdf", "application/octet-stream"),
            requirePersistentAccess = true
        ) ?: return@onIO null
        CreateMapFromUriCommand(context, uri, loadingIndicator).execute()
    }
}
