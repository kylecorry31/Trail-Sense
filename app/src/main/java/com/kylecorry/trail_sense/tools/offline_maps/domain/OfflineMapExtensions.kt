package com.kylecorry.trail_sense.tools.offline_maps.domain

/**
 * Get the total file size in bytes of the offline map
 */
fun OfflineMap.getFileSize(): Long {
    return files.sumOf { it.sizeBytes }
}

fun OfflineMap.isExternal(): Boolean {
    return files.any { it.isExternal }
}
