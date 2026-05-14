package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

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

    private val mapDataCache = lruCache<Tile, MapReadResult>(cacheSize)
    private val namedItemsCache = lruCache<Tile, MapReadResult>(cacheSize)
    private val poiDataCache = lruCache<Tile, MapReadResult>(cacheSize)
    private val dataTimestampCache = lruCache<Tile, Long>(cacheSize)
    private val supportsTileCache = lruCache<Tile, Boolean>(cacheSize)
    private val supportsFullTileCache = lruCache<Tile, Boolean>(cacheSize)
    private val supportsAreaCache = lruCache<Triple<BoundingBox, Byte, Boolean>, Boolean>(cacheSize)

    override fun boundingBox(): BoundingBox = boundingBoxCache

    override fun close() {
        clearCaches()
        delegate.close()
    }

    override fun getDataTimestamp(tile: Tile): Long {
        return dataTimestampCache.getOrPut(tile) { delegate.getDataTimestamp(tile) }
    }

    override fun readMapData(tile: Tile): MapReadResult {
        return mapDataCache.getOrPut(tile) { delegate.readMapData(tile) }
    }

    override fun readNamedItems(tile: Tile): MapReadResult {
        return namedItemsCache.getOrPut(tile) { delegate.readNamedItems(tile) }
    }

    override fun readPoiData(tile: Tile): MapReadResult {
        return poiDataCache.getOrPut(tile) { delegate.readPoiData(tile) }
    }

    override fun startPosition(): LatLong? = startPositionCache

    override fun startZoomLevel(): Byte? = startZoomLevelCache

    override fun supportsTile(tile: Tile): Boolean {
        return supportsTileCache.getOrPut(tile) { delegate.supportsTile(tile) }
    }

    override fun supportsFullTile(tile: Tile): Boolean {
        return supportsFullTileCache.getOrPut(tile) { delegate.supportsFullTile(tile) }
    }

    override fun supportsArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean {
        return supportsAreaCache.getOrPut(Triple(boundingBox, zoomLevel, false)) {
            delegate.supportsArea(boundingBox, zoomLevel)
        }
    }

    override fun supportsFullArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean {
        return supportsAreaCache.getOrPut(Triple(boundingBox, zoomLevel, true)) {
            delegate.supportsFullArea(boundingBox, zoomLevel)
        }
    }

    private fun clearCaches() {
        mapDataCache.clear()
        namedItemsCache.clear()
        poiDataCache.clear()
        dataTimestampCache.clear()
        supportsTileCache.clear()
        supportsFullTileCache.clear()
        supportsAreaCache.clear()
    }

    companion object {
        private const val DEFAULT_CACHE_SIZE = 50

        private fun <K, V> lruCache(maxSize: Int): LinkedHashMap<K, V> =
            object : LinkedHashMap<K, V>(maxSize + 1, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<K, V>) = size > maxSize
            }
    }
}
