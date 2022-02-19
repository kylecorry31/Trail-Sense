package com.kylecorry.trail_sense.shared.io

import android.net.Uri
import java.io.InputStream

interface UriService {
    suspend fun write(uri: Uri, data: String): Boolean
    suspend fun read(uri: Uri): String?
    suspend fun stream(uri: Uri): InputStream?
}