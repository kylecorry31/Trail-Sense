package com.kylecorry.trail_sense.shared.io

import android.net.Uri

interface UriService {
    suspend fun write(uri: Uri, data: String): Boolean
    suspend fun read(uri: Uri): String?
}