package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import androidx.collection.LruCache
import com.kylecorry.trail_sense.shared.andromeda_temp.getOrPut
import com.kylecorry.trail_sense.shared.andromeda_temp.getOrPutUnlockedProducer
import com.kylecorry.trail_sense.shared.concurrency.StripedLock
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult

@Suppress("TooManyFunctions")
class CachedMapsforgeMapDataStoreWrapper(
    private val delegate: MapDataStore,
    private val cacheSize: Int = DEFAULT_CACHE_SIZE
) : MapDataStore() {

    private val boundingBoxCache by lazy { delegate.boundingBox() }
    private val startPositionCache by lazy { delegate.startPosition() }
    private val startZoomLevelCache by lazy { delegate.startZoomLevel() }

    private val mapDataLock = StripedLock()
    private val mapDataCache = LruCache<Tile, MapReadResult>(cacheSize)

    private val namedItemsLock = StripedLock()
    private val namedItemsCache = LruCache<Tile, MapReadResult>(cacheSize)
    private val poiDataLock = StripedLock()
    private val poiDataCache = LruCache<Tile, MapReadResult>(cacheSize)
    private val dataTimestampLock = StripedLock()
    private val dataTimestampCache = LruCache<Tile, Long>(cacheSize)
    private val supportsTileLock = StripedLock()
    private val supportsTileCache = LruCache<Tile, Boolean>(cacheSize)
    private val supportsFullTileLock = StripedLock()
    private val supportsFullTileCache = LruCache<Tile, Boolean>(cacheSize)
    private val supportsAreaLock = StripedLock()
    private val supportsAreaCache = LruCache<Triple<BoundingBox, Byte, Boolean>, Boolean>(cacheSize)

    override fun boundingBox(): BoundingBox = boundingBoxCache

    override fun close() {
        clearCaches()
        delegate.close()
    }

    override fun getDataTimestamp(tile: Tile): Long {
        return dataTimestampCache.getOrPut(tile, lock = dataTimestampLock) { delegate.getDataTimestamp(tile) }
    }

    override fun readMapData(tile: Tile): MapReadResult {
        return mapDataCache.getOrPutUnlockedProducer(tile, mapDataLock) { delegate.readMapData(tile) }
    }

    override fun readNamedItems(tile: Tile): MapReadResult {
        return namedItemsCache.getOrPutUnlockedProducer(tile, namedItemsLock) { delegate.readNamedItems(tile) }
    }

    override fun readPoiData(tile: Tile): MapReadResult {
        return poiDataCache.getOrPutUnlockedProducer(tile, poiDataLock) { delegate.readPoiData(tile) }
    }

    override fun startPosition(): LatLong? = startPositionCache

    override fun startZoomLevel(): Byte? = startZoomLevelCache

    override fun supportsTile(tile: Tile): Boolean {
        return supportsTileCache.getOrPutUnlockedProducer(tile, supportsTileLock) { delegate.supportsTile(tile) }
    }

    override fun supportsFullTile(tile: Tile): Boolean {
        return supportsFullTileCache.getOrPutUnlockedProducer(tile, supportsFullTileLock) {
            delegate.supportsFullTile(tile)
        }
    }

    override fun supportsArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean {
        return supportsAreaCache.getOrPutUnlockedProducer(Triple(boundingBox, zoomLevel, false), supportsAreaLock) {
            delegate.supportsArea(boundingBox, zoomLevel)
        }
    }

    override fun supportsFullArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean {
        return supportsAreaCache.getOrPutUnlockedProducer(Triple(boundingBox, zoomLevel, true), supportsAreaLock) {
            delegate.supportsFullArea(boundingBox, zoomLevel)
        }
    }

    private fun clearCaches() {
        mapDataCache.evictAll()
        namedItemsCache.evictAll()
        poiDataCache.evictAll()
        dataTimestampCache.evictAll()
        supportsTileCache.evictAll()
        supportsFullTileCache.evictAll()
        supportsAreaCache.evictAll()
    }

    companion object {
        private const val DEFAULT_CACHE_SIZE = 50
    }
}
