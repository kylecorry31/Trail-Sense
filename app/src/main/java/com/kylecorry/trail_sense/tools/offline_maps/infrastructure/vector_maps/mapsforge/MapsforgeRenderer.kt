package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.labels.LabelStore
import org.mapsforge.map.layer.renderer.DatabaseRenderer
import org.mapsforge.map.rendertheme.RenderContext

class MapsforgeRenderer(
    private val dataStore: MapDataStore,
    graphicFactory: GraphicFactory,
    tileCache: TileCache,
    labelStore: LabelStore,
    private val mapReadResultModifiers: List<MapReadResultModifier> = emptyList()
) : DatabaseRenderer(
    dataStore,
    graphicFactory,
    tileCache,
    labelStore,
    true,
    true,
    null,
    true
) {
    override fun processReadMapData(renderContext: RenderContext?, mapReadResult: MapReadResult?) {
        if (renderContext != null && mapReadResult != null) {
            mapReadResultModifiers.forEach { it.process(renderContext, mapReadResult, dataStore) }
        }
        super.processReadMapData(renderContext, mapReadResult)
    }
}
