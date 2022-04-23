package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.kylecorry.andromeda.files.ExternalFiles
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.io.File
import java.util.*

object Files {
    private const val TEMP_DIR = "tmp"

    suspend fun copyToDirectory(context: Context, from: Uri, to: String): File? = onIO {
        val type = context.contentResolver.getType(from)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val filename = "$to/${UUID.randomUUID()}.$extension"
        val file = LocalFiles.getFile(context, filename, true)
        val stream = ExternalFiles.stream(context, from) ?: return@onIO null

        try {
            val saver = FileSaver()
            saver.save(stream, file)
        } catch (e: Exception) {
            return@onIO null
        }

        file
    }

    suspend fun copyToTemp(context: Context, from: Uri): File? {
        return copyToDirectory(context, from, TEMP_DIR)
    }

    suspend fun deleteTempFiles(context: Context) = onIO {
        val dir = LocalFiles.getDirectory(context, TEMP_DIR, false)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun createTempFile(context: Context, extension: String): File = onIO {
        val filename = "${TEMP_DIR}/${UUID.randomUUID()}.$extension"
        LocalFiles.getFile(context, filename, true)
    }

    fun getLocalPath(file: File): String {
        return file.path.substringAfter("files/")
    }

}