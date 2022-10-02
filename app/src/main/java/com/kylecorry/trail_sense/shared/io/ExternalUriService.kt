package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

class ExternalUriService(context: Context) : UriService {
    private val files = FileSubsystem.getInstance(context)
    override suspend fun write(uri: Uri, data: String): Boolean {
        return files.write(uri, data)
    }

    override suspend fun outputStream(uri: Uri): OutputStream? {
        return files.output(uri)
    }

    override suspend fun read(uri: Uri): String? {
        return files.read(uri)
    }

    override suspend fun inputStream(uri: Uri): InputStream? {
        return files.stream(uri)
    }
}