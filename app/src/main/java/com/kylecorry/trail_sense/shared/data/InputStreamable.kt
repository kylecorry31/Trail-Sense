package com.kylecorry.trail_sense.shared.data

import java.io.InputStream

fun interface InputStreamable {
    suspend fun getInputStream(): InputStream?
}