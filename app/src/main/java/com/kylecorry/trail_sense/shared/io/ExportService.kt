package com.kylecorry.trail_sense.shared.io

interface ExportService<T> {
    suspend fun export(data: T, filename: String): Boolean
}