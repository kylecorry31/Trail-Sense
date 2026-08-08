package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.trail_maps

import android.content.Context
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.trail_maps.TrailMap

class TrailMapExportService(
    private val context: Context,
    private val uriPicker: UriPicker
) : ExportService<TrailMap> {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun export(data: TrailMap, filename: String): Boolean {
        val uri = uriPicker.create(filename, MIME_TYPE) ?: return false
        val outputStream = files.output(uri) ?: return false
        val inputStream = files.fileInputStream(data.mapFile.path) ?: return false
        return try {
            val saver = FileSaver()
            saver.save(inputStream, outputStream)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val MIME_TYPE = "application/octet-stream"
    }
}
