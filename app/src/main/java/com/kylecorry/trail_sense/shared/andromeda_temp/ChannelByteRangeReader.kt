package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.luna.concurrency.onIO
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * Reads a file through a channel, which is opened on the first read and kept open until [close] (or reading the channel
 * fails, in which case the next read opens it again). Once closed, every read throws an [IOException].
 */
class ChannelByteRangeReader(private val openChannel: suspend () -> OpenChannel) : ByteRangeReader {

    /**
     * A file that is [size] bytes long, starting at [start] in [channel]. Closing [resource] closes the channel.
     */
    class OpenChannel(val channel: FileChannel, val start: Long, val size: Long, val resource: Closeable)

    private var current: OpenChannel? = null
    private var isClosed = false

    override suspend fun read(offset: Long, length: Int): ByteArray = onIO {
        val open = getOrOpen()
        // A range past the end is the caller's mistake, so the channel stays open
        if (offset < 0 || length < 0 || offset + length > open.size) {
            throw EOFException()
        }
        try {
            ByteRangeReader.readFully(open.channel, open.start, open.size, offset, length)
        } catch (e: IOException) {
            close(open)
            throw e
        }
    }

    override fun close() {
        synchronized(this) {
            isClosed = true
            current?.resource?.close()
            current = null
        }
    }

    private suspend fun getOrOpen(): OpenChannel {
        synchronized(this) {
            if (isClosed) {
                throw IOException("The reader is closed")
            }
            current?.let { return it }
        }
        val opened = openChannel()
        synchronized(this) {
            // The reader may have been closed, or another read may have opened it, in the meantime
            if (isClosed) {
                opened.resource.close()
                throw IOException("The reader is closed")
            }
            current?.let {
                opened.resource.close()
                return it
            }
            current = opened
            return opened
        }
    }

    private fun close(open: OpenChannel) {
        synchronized(this) {
            open.resource.close()
            if (current === open) {
                current = null
            }
        }
    }
}
