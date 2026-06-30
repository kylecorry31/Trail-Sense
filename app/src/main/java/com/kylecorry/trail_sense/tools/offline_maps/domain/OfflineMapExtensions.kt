package com.kylecorry.trail_sense.tools.offline_maps.domain

/**
 * Get the total file size in bytes of the offline map
 */
fun OfflineMap.getFileSize(): Long {
    return files.sumOf { it.sizeBytes }
}

/**
 * Determines if this is an external map
 */
fun OfflineMap.isExternal(): Boolean {
    return files.any { it.isExternal }
}
