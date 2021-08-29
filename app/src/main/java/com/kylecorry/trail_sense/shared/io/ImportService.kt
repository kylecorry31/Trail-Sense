package com.kylecorry.trail_sense.shared.io

interface ImportService<T> {
    suspend fun import(): T?
}