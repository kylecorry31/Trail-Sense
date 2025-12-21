package com.kylecorry.trail_sense.tools.paths.map_layers

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.BackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class PathLayer : GeoJsonLayer<PathGeoJsonSource>(PathGeoJsonSource(), layerId = LAYER_ID) {

    private val pathService = AppServiceRegistry.get<PathService>()
    private val task = BackgroundTask {
        listenerRunner.skipIfRunning {
            pathService.getPaths().collect {
                wasBacktrackOn = pathService.getBacktrackPathId() != null
                // Paths changed, so we need to reload the paths
                reload()
            }
        }
    }
    private val listenerRunner = CoroutineQueueRunner()
    private var wasBacktrackOn = false

    private val prefs = AppServiceRegistry.get<UserPreferences>()

    init {
        renderer.configureLineStringRenderer(shouldRenderWithDrawLines = prefs.navigation.useFastPathRendering)
    }

    private val onLocationChanged = { _: Bundle ->
        if (wasBacktrackOn) {
            invalidate()
        }
        true
    }

    override fun start() {
        Tools.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED, onLocationChanged)
        task.start()
    }

    override fun stop() {
        super.stop()
        Tools.unsubscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED, onLocationChanged)
        listenerRunner.cancel()
        task.stop()
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        val backgroundColorId = preferences.getLong(PathMapLayerPreferences.BACKGROUND_COLOR)
        renderer.configureLineStringRenderer(
            backgroundColor = PathBackgroundColor.entries.withId(backgroundColorId)
        )
    }

    fun reload() {
        source.reload()
        invalidate()
    }

    companion object {
        const val LAYER_ID = "path"
    }
}