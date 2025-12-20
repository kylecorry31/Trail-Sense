package com.kylecorry.trail_sense.tools.paths.map_layers

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PathLayer : GeoJsonLayer<PathGeoJsonSource>(PathGeoJsonSource()) {

    private val pathService = AppServiceRegistry.get<PathService>()
    private var scope: CoroutineScope? = null
    private val listenerRunner = CoroutineQueueRunner()
    private var wasBacktrackOn = false

    private val onLocationChanged = { _: Bundle ->
        if (wasBacktrackOn) {
            invalidate()
        }
        true
    }

    override fun start() {
        Tools.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED, onLocationChanged)
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Default)
        scope?.launch {
            listenerRunner.skipIfRunning {
                pathService.getPaths().collect {
                    wasBacktrackOn = pathService.getBacktrackPathId() != null
                    // Paths changed, so we need to reload the paths
                    reload()
                }
            }
        }
    }

    override fun stop() {
        super.stop()
        Tools.unsubscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED, onLocationChanged)
        listenerRunner.cancel()
        scope?.cancel()
        scope = null
    }

    override fun setPreferences(preferences: Bundle) {
        percentOpacity = preferences.getInt(BaseMapLayerPreferences.OPACITY) / 100f
        val backgroundColorId = preferences.getLong(PathMapLayerPreferences.BACKGROUND_COLOR)
        renderer.configureLineStringRenderer(
            backgroundColor = PathBackgroundColor.entries.withId(backgroundColorId)
        )
    }

    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        renderer.configureLineStringRenderer(shouldRenderWithDrawLines = shouldRenderWithDrawLines)
    }

    fun reload() {
        source.reload()
        invalidate()
    }
}