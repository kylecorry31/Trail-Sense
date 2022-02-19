package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.files.ExternalFiles
import java.io.InputStream

class ExternalUriService(private val context: Context) : UriService {
    override suspend fun write(uri: Uri, data: String): Boolean {
        return ExternalFiles.write(context, uri, data)
    }

    override suspend fun read(uri: Uri): String? {
        return ExternalFiles.read(context, uri)
    }

    override suspend fun stream(uri: Uri): InputStream? {
        return ExternalFiles.stream(context, uri)
    }
}