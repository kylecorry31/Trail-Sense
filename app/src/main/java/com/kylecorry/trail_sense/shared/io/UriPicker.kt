package com.kylecorry.trail_sense.shared.io

import android.net.Uri
import com.kylecorry.luna.result.Result

interface UriPicker {
    suspend fun open(types: List<String>, requirePersistentAccess: Boolean = false): Result<Uri, UriPickerError>
    suspend fun create(filename: String, type: String): Uri?
}
