package com.kylecorry.trail_sense.tools.paths.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathLoader
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.asMappable
import kotlinx.coroutines.flow.first

class PathGeoJsonSource : GeoJsonSource {

    private val context = AppServiceRegistry.get<Context>()
    private val pathService = AppServiceRegistry.get<PathService>()
    private val pathLoader = PathLoader(pathService)
    private var paths = emptyList<Path>()
    private var loaded = false

    override suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int
    ): GeoJsonObject? {
        // If paths haven't been loaded yet, load them
        if (paths.isEmpty()) {
            paths = pathService.getPaths().first().filter { it.style.visible }
        }

        pathLoader.update(paths, bounds, bounds, !loaded)
        loaded = true

        val points = pathLoader.getPointsWithBacktrack(context)

        val mappablePaths = points.mapNotNull {
            val path =
                paths.firstOrNull { p -> p.id == it.key } ?: return@mapNotNull null

            it.value.asMappable(context, path)
        }

        return GeoJsonFeatureCollection(mappablePaths.map {
            GeoJsonFeature.lineString(
                it.points.map { point -> point.coordinate },
                it.id,
                it.name,
                it.style,
                it.color,
                it.thicknessScale
            )
        })
    }
    
    fun reload() {
        loaded = false
        paths = emptyList()
    }
}