package com.kylecorry.trail_sense.shared.io

interface IOService<T> {
    suspend fun export(data: T, filename: String): Boolean
    suspend fun import(): T?
}