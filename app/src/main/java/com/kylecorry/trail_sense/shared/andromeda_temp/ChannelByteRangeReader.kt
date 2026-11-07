package com.kylecorry.trail_sense.shared.andromeda_temp

import com.kylecorry.luna.concurrency.onIO
import java.io.Closeable
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * Reads a file through a channel, which is opened on the first read and kept open until [close] (or a read fails, in
 * which case the next read opens it again).
 */
class ChannelByteRangeReader(private val openChannel: suspend () -> OpenChannel) : ByteRangeReader {

    /**
     * A file that is [size] bytes long, starting at [start] in [channel]. Closing [resource] closes the channel.
     */
    class OpenChannel(val channel: FileChannel, val start: Long, val size: Long, val resource: Closeable)

    private var current: OpenChannel? = null

    override suspend fun read(offset: Long, length: Int): ByteArray = onIO {
        val open = getOrOpen()
        try {
            ByteRangeReader.readFully(open.channel, open.start, open.size, offset, length)
        } catch (e: IOException) {
            close(open)
            throw e
        }
    }

    override fun close() {
        synchronized(this) {
            current?.resource?.close()
            current = null
        }
    }

    private suspend fun getOrOpen(): OpenChannel {
        synchronized(this) {
            current?.let { return it }
        }
        val opened = openChannel()
        synchronized(this) {
            // Another read may have opened it in the meantime
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
