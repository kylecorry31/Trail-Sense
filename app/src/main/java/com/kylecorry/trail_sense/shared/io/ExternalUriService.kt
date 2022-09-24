package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.files.ExternalFileSystem
import com.kylecorry.trail_sense.shared.extensions.onIO
import java.io.InputStream
import java.io.OutputStream

class ExternalUriService(private val context: Context) : UriService {
    private val externalFiles = ExternalFileSystem(context)
    override suspend fun write(uri: Uri, data: String): Boolean {
        return externalFiles.write(uri, data)
    }

    override suspend fun outputStream(uri: Uri): OutputStream? {
        return getOutputStream(context, uri)
    }

    override suspend fun read(uri: Uri): String? {
        return externalFiles.read(uri)
    }

    override suspend fun inputStream(uri: Uri): InputStream? {
        return externalFiles.stream(uri)
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