package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.result.Result
import com.kylecorry.luna.result.andThen
import com.kylecorry.luna.result.mapError
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.shared.io.UriPickerError
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapRequest

class CreateMapFromFileCommand(
    private val context: Context,
    private val uriPicker: UriPicker
) : ICreateMapCommand {
    override suspend fun execute(): Result<CreateOfflineMapRequest, CreateOfflineMapError> = onIO {
        uriPicker.open(
            listOf("image/*", "application/pdf", "application/octet-stream"),
            requirePersistentAccess = true
        ).mapError {
            when (it) {
                UriPickerError.AccessDenied -> CreateOfflineMapError.AccessDenied
                UriPickerError.Cancelled -> CreateOfflineMapError.Cancelled
            }
        }.andThen {
            CreateMapFromUriCommand(context, it).execute()
        }
    }
}
