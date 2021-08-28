package com.kylecorry.trail_sense.shared.io

import android.net.Uri

interface UriPicker {
    suspend fun open(types: List<String>): Uri?
    suspend fun create(filename: String, type: String): Uri?
}