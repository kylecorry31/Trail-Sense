package com.kylecorry.trail_sense.shared.uri

import android.net.Uri

interface IUriDecoder<T> {
    fun decode(uri: Uri): T?
}