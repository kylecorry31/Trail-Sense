package com.kylecorry.trail_sense.shared.io

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

interface UriService {
    suspend fun write(uri: Uri, data: String): Boolean
    suspend fun outputStream(uri: Uri): OutputStream?
    suspend fun read(uri: Uri): String?
    suspend fun inputStream(uri: Uri): InputStream?
}