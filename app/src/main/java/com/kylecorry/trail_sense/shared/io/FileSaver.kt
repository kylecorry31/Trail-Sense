package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.core.tryOrNothing
import java.io.*

class FileSaver(private val autoClose: Boolean = true) {

    fun save(input: InputStream, output: OutputStream) {
        try {
            val buf = ByteArray(1024)
            var len: Int
            while (input.read(buf).also { len = it } > 0) {
                output.write(buf, 0, len)
            }
        } finally {
            if (autoClose) {
                tryOrNothing { input.close() }
                tryOrNothing { output.close() }
            }
        }
    }

    fun save(input: InputStream, output: File) {
        FileOutputStream(output).use {
            save(input, it)
        }
    }

    fun save(input: File, output: File) {
        FileInputStream(input).use { inputStream ->
            FileOutputStream(output).use { outputStream ->
                save(inputStream, outputStream)
            }
        }
    }

}