package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile

class ByteRangeReaderTest {

    @TempDir
    lateinit var directory: File

    private val bytes = ByteArray(100) { it.toByte() }

    private fun file(): File {
        return File(directory, "data.bin").also { it.writeBytes(bytes) }
    }

    @Test
    fun readsRangesOfAFile() = runBlocking {
        ByteRangeReader.fromFile(file()).use { reader ->
            assertArrayEquals(bytes.copyOfRange(10, 15), reader.read(10, 5))
            assertArrayEquals(bytes.copyOfRange(0, 100), reader.read(0, 100))
            assertArrayEquals(ByteArray(0), reader.read(100, 0))
        }
    }

    @Test
    fun throwsPastTheEndOfAFile() {
        ByteRangeReader.fromFile(file()).use { reader ->
            assertThrows(EOFException::class.java) { runBlocking { reader.read(98, 5) } }
            assertThrows(EOFException::class.java) { runBlocking { reader.read(-1, 5) } }
        }
    }

    @Test
    fun readsRangesOfBytes() {
        val reader = ByteRangeReader.fromBytes(bytes)
        assertArrayEquals(bytes.copyOfRange(90, 100), runBlocking { reader.read(90, 10) })
        assertThrows(EOFException::class.java) { runBlocking { reader.read(95, 10) } }
    }

    @Test
    fun readsAFileThatStartsPartWayThroughTheChannel() {
        // Like an asset inside an APK, where the file is a piece of a larger one
        val reader = ChannelByteRangeReader {
            val file = RandomAccessFile(file(), "r")
            ChannelByteRangeReader.OpenChannel(file.channel, 20, 30, file)
        }
        reader.use {
            assertArrayEquals(bytes.copyOfRange(25, 30), runBlocking { it.read(5, 5) })
            assertThrows(EOFException::class.java) { runBlocking { it.read(28, 5) } }
        }
    }

    @Test
    fun keepsTheChannelOpenUntilClosed() = runBlocking {
        var opens = 0
        var closes = 0
        val reader = ChannelByteRangeReader {
            opens++
            val file = RandomAccessFile(file(), "r")
            ChannelByteRangeReader.OpenChannel(file.channel, 0, file.length(), Closeable {
                closes++
                file.close()
            })
        }

        reader.read(0, 5)
        reader.read(5, 5)
        assertEquals(1, opens)

        // A failed read (here past the end) opens the channel again next time
        assertThrows(EOFException::class.java) { runBlocking { reader.read(99, 5) } }
        reader.read(0, 5)
        assertEquals(2, opens)

        reader.close()
        assertEquals(2, closes)
    }
}
