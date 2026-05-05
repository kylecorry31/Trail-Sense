package com.kylecorry.trail_sense.tools.map.infrastructure.mapsforge

import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tile
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult

@Suppress("TooManyFunctions")
class MapsforgeMapDataStoreWrapper(
    private val delegate: MapDataStore,
    private val poiModifiers: List<MapsforgePoiModifier>
) : MapDataStore() {

    override fun boundingBox(): BoundingBox {
        return delegate.boundingBox()
    }

    override fun close() {
        delegate.close()
    }

    override fun getDataTimestamp(tile: Tile): Long {
        return delegate.getDataTimestamp(tile)
    }

    override fun readMapData(tile: Tile): MapReadResult {
        return delegate.readMapData(tile).withModifiedPois()
    }

    override fun readMapData(upperLeft: Tile, lowerRight: Tile): MapReadResult {
        return delegate.readMapData(upperLeft, lowerRight).withModifiedPois()
    }

    override fun readNamedItems(tile: Tile): MapReadResult {
        return delegate.readNamedItems(tile).withModifiedPois()
    }

    override fun readPoiData(tile: Tile): MapReadResult {
        return delegate.readPoiData(tile).withModifiedPois()
    }

    override fun startPosition(): LatLong? {
        return delegate.startPosition()
    }

    override fun startZoomLevel(): Byte? {
        return delegate.startZoomLevel()
    }

    override fun supportsTile(tile: Tile): Boolean {
        return delegate.supportsTile(tile)
    }

    override fun supportsFullTile(tile: Tile): Boolean {
        return delegate.supportsFullTile(tile)
    }

    override fun supportsArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean {
        return delegate.supportsArea(boundingBox, zoomLevel)
    }

    override fun supportsFullArea(boundingBox: BoundingBox, zoomLevel: Byte): Boolean {
        return delegate.supportsFullArea(boundingBox, zoomLevel)
    }

    private fun MapReadResult.withModifiedPois(): MapReadResult {
        if (poiModifiers.isNotEmpty()){
            pois.replaceAll { poi ->
                poiModifiers.fold(poi) { modified, modifier ->
                    modifier.modify(modified)
                }
            }
        }
        return this
    }
}
