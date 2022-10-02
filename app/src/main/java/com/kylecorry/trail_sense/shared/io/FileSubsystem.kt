package com.kylecorry.trail_sense.shared.io

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.kylecorry.andromeda.core.bitmap.BitmapUtils
import com.kylecorry.andromeda.files.ExternalFileSystem
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.andromeda.files.LocalFileSystem
import com.kylecorry.trail_sense.shared.extensions.ifDebug
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.tools.maps.infrastructure.ImageSaver
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class FileSubsystem private constructor(private val context: Context) {

    private val external = ExternalFileSystem(context)
    private val local = LocalFileSystem(context)

    fun bitmap(path: String, maxSize: Size? = null): Bitmap {
        return if (maxSize != null) {
            BitmapUtils.decodeBitmapScaled(get(path).path, maxSize.width, maxSize.height)
        } else {
            BitmapFactory.decodeFile(get(path).path)
        }
    }

    fun get(path: String, create: Boolean = false): File {
        return local.getFile(path, create)
    }

    fun delete(path: String) {
        local.delete(path)
    }

    suspend fun stream(uri: Uri): InputStream? = onIO {
        external.stream(uri)
    }

    suspend fun write(uri: Uri, data: String): Boolean = onIO {
        external.write(uri, data)
    }

    suspend fun read(uri: Uri): String? = onIO {
        external.read(uri)
    }

    suspend fun output(uri: Uri): OutputStream? = onIO {
        external.outputStream(uri)
    }

    suspend fun save(
        path: String,
        bitmap: Bitmap,
        quality: Int = 90,
        recycleOnSave: Boolean = false
    ) = onIO {
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            FileOutputStream(get(path, create = true)).use { out ->
                ImageSaver().save(bitmap, out, quality)
            }
        } finally {
            if (recycleOnSave) {
                bitmap.recycle()
            }
        }
    }

    suspend fun rename(
        fromPath: String,
        toPath: String
    ): Boolean = onIO {
        val renamed = get(toPath)
        get(fromPath).renameTo(renamed)
    }

    fun imageSize(path: String): Size {
        val file = get(path)
        val size = BitmapUtils.getBitmapSize(file.path)
        return Size(size.first, size.second)
    }

    fun size(path: String): Long {
        return get(path).length()
    }

    fun uri(path: String, create: Boolean = false): Uri {
        return get(path, create).toUri()
    }

    suspend fun copyToLocal(uri: Uri, directory: String): File? = onIO {
        val type = context.contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val filename = "$directory/${UUID.randomUUID()}.$extension"
        val file = get(filename, true)
        val stream = stream(uri) ?: return@onIO null

        try {
            val saver = FileSaver()
            saver.save(stream, file)
        } catch (e: Exception) {
            return@onIO null
        }

        file
    }

    suspend fun copyToTemp(from: Uri): File? {
        return copyToLocal(from, TEMP_DIR)
    }

    suspend fun clearTemp() = onIO {
        local.delete(TEMP_DIR, true)
    }

    suspend fun createTemp(extension: String): File = onIO {
        val filename = "${TEMP_DIR}/${UUID.randomUUID()}.$extension"
        get(filename, true)
    }

    fun getLocalPath(file: File): String {
        return local.getRelativePath(file)
    }

    fun writeDebug(filename: String, text: String) {
        ifDebug {
            local.write("debug/$filename", text)
        }
    }

    companion object {
        private const val TEMP_DIR = "tmp"

        @SuppressLint("StaticFieldLeak")
        private var instance: FileSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): FileSubsystem {
            if (instance == null) {
                instance = FileSubsystem(context.applicationContext)
            }
            return instance!!
        }

    }

}