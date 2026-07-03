package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest

class CreateMapFromFileCommand(
    private val context: Context,
    private val uriPicker: UriPicker
) : ICreateMapCommand {
    override suspend fun execute(): CreateOfflineMapRequest? = onIO {
        val uri = uriPicker.open(
            listOf("image/*", "application/pdf", "application/octet-stream"),
            requirePersistentAccess = true
        ) ?: return@onIO null
        CreateMapFromUriCommand(context, uri).execute()
    }
}
