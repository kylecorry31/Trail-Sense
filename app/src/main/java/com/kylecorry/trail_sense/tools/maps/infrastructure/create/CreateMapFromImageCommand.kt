package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.kylecorry.andromeda.files.ExternalFiles
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.io.FileSaver
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo
import java.io.InputStream
import java.util.*

class CreateMapFromImageCommand(private val context: Context, private val repo: IMapRepo) {
    suspend fun execute(uri: Uri): Map? = onIO {
        val defaultName = context.getString(android.R.string.untitled)
        val type = context.contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val filename = "maps/" + UUID.randomUUID().toString() + "." + extension
        val stream = ExternalFiles.stream(context, uri) ?: return@onIO null

        try {
            copyToLocalStorage(stream, filename)
        } catch (e: Exception) {
            return@onIO null
        }

        val map = Map(
            0,
            defaultName,
            filename,
            emptyList(),
            warped = false,
            rotated = false
        )

        val id = repo.addMap(map)
        map.copy(id = id)
    }

    private fun copyToLocalStorage(stream: InputStream, filename: String) {
        val saver = FileSaver()
        saver.save(stream, LocalFiles.getFile(context, filename))
    }
}