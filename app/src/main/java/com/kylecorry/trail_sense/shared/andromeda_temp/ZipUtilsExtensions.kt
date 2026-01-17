package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.files.FileSaver
import com.kylecorry.andromeda.files.ZipUtils
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

inline fun ZipInputStream.forEach(fn: (entry: ZipEntry) -> Boolean) {
    use {
        var entry = nextEntry
        var shouldContinue = true
        while (entry != null && shouldContinue) {
            shouldContinue = fn(entry)
            entry = nextEntry
        }
        tryOrNothing {
            closeEntry()
        }
    }
}

inline fun ZipUtils.unzip(
    fromStream: InputStream,
    toDirectory: File,
    maxCount: Int = Int.MAX_VALUE,
    onUnzip: (file: File) -> Unit = {}
) {
    val zip = ZipInputStream(fromStream)
    var count = 0
    val saver = FileSaver(false)
    zip.forEach {
        val dest = File(toDirectory, it.name)
        if (it.isDirectory) {
            if (!dest.exists()) {
                dest.mkdirs()
            }
        } else {
            val parent = dest.parentFile
            if (parent?.exists() == false) {
                parent.mkdirs()
            }
            if (!dest.exists()) {
                dest.createNewFile()
            }

            saver.save(zip, dest)
            onUnzip(dest)
        }
        count++
        count < maxCount
    }
}