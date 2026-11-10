package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.luna.concurrency.SingleFlight
import com.kylecorry.trail_sense.shared.andromeda_temp.ByteRangeReader
import com.kylecorry.trail_sense.shared.andromeda_temp.RawDeflate
import kotlinx.coroutines.CancellationException
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A file of points on the globe, each with some values, grouped into square cells and stored in compressed blocks (see
 * docs/geo-point-file-format.md in the earth model generator for the format). What the values mean is up to the caller,
 * using the properties of the file.
 *
 * Only the blocks around a location are read (and kept for next time), so a large file can be used with little memory.
 */
class GeoPointFile private constructor(
    private val reader: ByteRangeReader,
    private val metadata: GeoPointMetadata,
    private val directory: GeoPointDirectory,
    cachedBlocks: Int
) : Closeable {

    private val decoder = GeoPointBlockDecoder(metadata.grid, metadata.fields)
    private val cache = MemoryLRUCache<Int, Map<Int, GeoPointCell>>(cachedBlocks)
    private val reads = SingleFlight<Int, Map<Int, GeoPointCell>?>()

    fun getNumberProperty(name: String): Double? {
        return (metadata.properties[name] as? Number)?.toDouble()
    }

    fun getStringProperty(name: String): String? {
        return metadata.properties[name] as? String
    }

    fun hasField(name: String): Boolean {
        return metadata.fields.any { it.name == name }
    }

    override fun close() {
        reader.close()
    }

    /**
     * The cells with points that cover an area. The area can extend past the antimeridian (a west of -190 is 170).
     */
    suspend fun getCells(south: Double, north: Double, west: Double, east: Double): List<GeoPointCell> {
        // Each block is read once, however many of its cells are needed
        val cellsByBlock = metadata.grid.getCells(south, north, west, east)
            .groupBy { directory.findBlock(it) }
            .filterKeys { it >= 0 }
        return cellsByBlock.flatMap { (block, cells) ->
            val blockCells = getBlockCells(block) ?: return@flatMap emptyList()
            cells.mapNotNull { blockCells[it] }
        }
    }

    private suspend fun getBlockCells(block: Int): Map<Int, GeoPointCell>? {
        cache.get(block)?.let { return it }
        // A block that could not be read is not kept, so it is tried again next time
        val cells = reads.invoke(block) { readBlock(block) } ?: return null
        cache.put(block, cells)
        return cells
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun readBlock(block: Int): Map<Int, GeoPointCell>? {
        return try {
            val length = directory.getLength(block)
            require(length <= MAX_BLOCK_BYTES) { "The block is too large" }
            val compressed = reader.read(directory.getOffset(block), length.toInt())
            val data = RawDeflate.inflate(compressed, MAX_BLOCK_BYTES)
            decoder.decode(data, directory.getFirstCell(block), directory.getLastCell(block))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A block that cannot be read just has no points
            null
        }
    }

    companion object {
        private val MAGIC = "TSGP".toByteArray(Charsets.US_ASCII)

        // Files of a newer version may be laid out differently, so they are not used
        private const val FORMAT_VERSION = 1
        private const val HEADER_BYTES = 16
        private const val MAX_METADATA_BYTES = 64 * 1024
        private const val MAX_BLOCK_BYTES = 1024 * 1024
        const val DEFAULT_CACHED_BLOCKS = 8

        /**
         * Opens a file by reading its header, metadata, and block directory, or returns null if it is not a file this
         * version understands (including one that is cut off). Throws an [IOException] if it could not be read, which
         * may work if tried again. Up to [cachedBlocks] decompressed blocks are kept in memory.
         *
         * The file owns the reader once it is open. If it is not opened, the reader is closed.
         */
        @Suppress("TooGenericExceptionCaught")
        suspend fun open(reader: ByteRangeReader, cachedBlocks: Int = DEFAULT_CACHED_BLOCKS): GeoPointFile? {
            val file = try {
                read(reader, cachedBlocks)
            } catch (e: Throwable) {
                reader.close()
                throw e
            }
            if (file == null) {
                reader.close()
            }
            return file
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private suspend fun read(reader: ByteRangeReader, cachedBlocks: Int): GeoPointFile? {
            return try {
                val header = ByteBuffer.wrap(reader.read(0, HEADER_BYTES)).order(ByteOrder.LITTLE_ENDIAN)
                val magic = ByteArray(MAGIC.size).also { header.get(it) }
                val version = header.get().toInt() and 0xFF
                val metadataBytes = header.getInt(8)
                val blockCount = header.getInt(12)
                require(magic.contentEquals(MAGIC) && version == FORMAT_VERSION) { "Not a supported file" }
                require(metadataBytes in 0..MAX_METADATA_BYTES) { "The metadata is too large" }

                val json = String(reader.read(HEADER_BYTES.toLong(), metadataBytes), Charsets.UTF_8)
                val metadata = GeoPointMetadata.parse(json)
                val directoryStart = HEADER_BYTES.toLong() + metadataBytes
                val directory = GeoPointDirectory.read(reader, directoryStart, blockCount, metadata.grid.cellCount)
                GeoPointFile(reader, metadata, directory, cachedBlocks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: EOFException) {
                null
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                // Includes a file that does not pass the checks
                null
            }
        }
    }
}
