package com.kylecorry.trail_sense.shared.uri

import android.net.Uri

interface IUriEncoder<T> {
    fun encode(value: T): Uri
}