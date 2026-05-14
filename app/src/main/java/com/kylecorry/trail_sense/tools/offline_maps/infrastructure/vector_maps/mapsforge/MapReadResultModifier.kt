package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.datastore.MapReadResult
import org.mapsforge.map.rendertheme.RenderContext

interface MapReadResultModifier {
    fun process(
        renderContext: RenderContext,
        mapReadResult: MapReadResult,
        mapDataStore: MapDataStore
    )
}
