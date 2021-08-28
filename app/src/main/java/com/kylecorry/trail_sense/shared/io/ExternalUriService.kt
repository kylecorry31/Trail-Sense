package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import com.kylecorry.andromeda.files.ExternalFiles

class ExternalUriService(private val context: Context) : UriService {
    override suspend fun write(uri: Uri, data: String): Boolean {
        return ExternalFiles.write(context, uri, data)
    }

    override suspend fun read(uri: Uri): String? {
        return ExternalFiles.read(context, uri)
    }
}