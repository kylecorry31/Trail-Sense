package com.kylecorry.trail_sense.settings.backup

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class BackupServiceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.cacheDir, "truncated_backup.zip")

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun restoreTruncatedBackupThrowsInvalidBackup() {
        // Without an app version file, a zip that somehow passes listing fails as a newer backup and never restores
        ZipOutputStream(file.outputStream()).use {
            it.putNextEntry(ZipEntry("files/a.txt"))
            it.write("a".toByteArray())
            it.closeEntry()
            it.putNextEntry(ZipEntry("files/b.bin"))
            it.write(Random(0).nextBytes(200_000))
            it.closeEntry()
        }
        RandomAccessFile(file, "rw").use { it.setLength(it.length() / 2) }

        val service = BackupService(context)
        assertThrows(BackupService.InvalidBackupException::class.java) {
            runBlocking { service.restore(Uri.fromFile(file)) }
        }
    }
}
