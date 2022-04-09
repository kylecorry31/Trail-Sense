package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.files.ExternalFiles
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.io.InputStream
import java.io.OutputStream

class ExternalUriService(private val context: Context) : UriService {
    override suspend fun write(uri: Uri, data: String): Boolean {
        return ExternalFiles.write(context, uri, data)
    }

    override suspend fun outputStream(uri: Uri): OutputStream? {
        return getOutputStream(context, uri)
    }

    override suspend fun read(uri: Uri): String? {
        return ExternalFiles.read(context, uri)
    }

    override suspend fun inputStream(uri: Uri): InputStream? {
        return ExternalFiles.stream(context, uri)
    }

    // TODO: Move this to external files
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getOutputStream(context: Context, uri: Uri): OutputStream? {
        return onIO {
            try {
                context.contentResolver.openOutputStream(uri)
            } catch (e: Exception) {
                null
            }
        }
    }
}