package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.isClickable
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

typealias OnGeoJsonFeatureClickListener = (GeoJsonFeature) -> Unit

open class GeoJsonLayer<T : GeoJsonSource>(
    protected val source: T,
    override val layerId: String,
    private val minZoomLevel: Int? = null,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    override val isTimeDependent: Boolean = false,
    private val refreshInterval: Duration? = null,
    private val refreshBroadcasts: List<String> = emptyList()
) : IAsyncLayer {
    val renderer = GeoJsonRenderer()
    private var isInvalid = true
    private var updateListener: (() -> Unit)? = null
    private var onFeatureClick: OnGeoJsonFeatureClickListener? = null
    protected var layerPreferences: Bundle = bundleOf()
    private val refreshTimer = refreshInterval?.let { CoroutineTimer { refresh() } }

    private var _timeOverride: Instant? = null
    private var _renderTime: Instant = Instant.now()

    override fun setTime(time: Instant?) {
        _timeOverride = time
        refresh()
    }

    protected fun refresh() {
        _renderTime = _timeOverride ?: Instant.now()
        invalidate()
        notifyListeners()
    }

    private fun onRefreshBroadcastReceived(data: Bundle): Boolean {
        refresh()
        return true
    }

    init {
        val prefs = AppServiceRegistry.get<UserPreferences>()
        renderer.configureLineStringRenderer(shouldRenderWithDrawLines = prefs.navigation.useFastPathRendering)
        renderer.setOnClickListener(this::onClick)
        taskRunner.addTask { context, viewBounds, bounds, projection ->
            isInvalid = false
            val zoomLevel = projection.zoom.roundToInt()
            try {
                if (minZoomLevel != null) {
                    if (zoomLevel < minZoomLevel) {
                        renderer.setGeoJsonObject(GeoJsonFeatureCollection(emptyList()))
                        return@addTask
                    }
                }

                val params = bundleOf(MapLayerParams.PARAM_TIME to _renderTime.toEpochMilli())
                params.putBundle(MapLayerParams.PARAM_PREFERENCES, layerPreferences)
                val obj =
                    source.load(context, bounds, zoomLevel, params) ?: GeoJsonFeatureCollection(
                        emptyList()
                    )
                renderer.setGeoJsonObject(obj)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                isInvalid = true
            }
        }
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
        renderer.setHasUpdateListener(listener)
    }

    override fun setPreferences(preferences: Bundle) {
        layerPreferences = Bundle(preferences)
        percentOpacity = preferences.getInt(
            DefaultMapLayerDefinitions.OPACITY,
            DefaultMapLayerDefinitions.DEFAULT_OPACITY
        ) / 100f
        // TODO: Eventually make this a geojson feature property instead of a global setting
        if (preferences.containsKey(DefaultMapLayerDefinitions.BACKGROUND_COLOR)) {
            val backgroundColorId =
                preferences.getString(DefaultMapLayerDefinitions.BACKGROUND_COLOR)?.toLongOrNull()
            renderer.configureLineStringRenderer(
                backgroundColor = PathBackgroundColor.entries.withId(backgroundColorId ?: 0)
                    ?: PathBackgroundColor.None
            )
        }
        if (preferences.containsKey(DefaultMapLayerDefinitions.SHOW_LABELS)) {
            renderer.configureLineStringRenderer(
                shouldRenderLabels = preferences.getBoolean(
                    DefaultMapLayerDefinitions.SHOW_LABELS,
                    true
                )
            )
        }
    }

    override fun draw(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        renderer.draw(context, drawer, map)
        taskRunner.scheduleUpdate(
            context,
            drawer.getBounds(45f),
            map.mapBounds,
            map.mapProjection,
            isInvalid
        )
    }

    override fun drawOverlay(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        isInvalid = true
        renderer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return renderer.onClick(drawer, map, pixel)
    }

    fun setOnFeatureClickListener(listener: OnGeoJsonFeatureClickListener?) {
        onFeatureClick = listener
    }

    open fun onClick(feature: GeoJsonFeature): Boolean {
        if (onFeatureClick == null || !feature.isClickable()) {
            return false
        }
        onFeatureClick?.invoke(feature)
        return true
    }

    override fun start() {
        Tools.subscribe(
            MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED,
            this::onSelectionBroadcast
        )
        refreshBroadcasts.forEach {
            Tools.subscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshInterval?.let { refreshTimer?.interval(it, it) }
    }

    override fun stop() {
        Tools.unsubscribe(
            MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED,
            this::onSelectionBroadcast
        )
        refreshBroadcasts.forEach {
            Tools.unsubscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshTimer?.stop()
        taskRunner.stop()
    }

    private fun onSelectionBroadcast(bundle: Bundle): Boolean {
        val broadcastLayerId =
            bundle.getString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_LAYER_ID)
        if (broadcastLayerId == layerId) {
            val featureId = bundle.getString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_FEATURE_ID)
            renderer.setSelectedFeature(featureId)
        }
        return true
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override var percentOpacity: Float = 1f
}
