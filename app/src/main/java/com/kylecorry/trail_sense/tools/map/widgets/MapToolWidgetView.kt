package com.kylecorry.trail_sense.tools.map.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import kotlinx.coroutines.delay

class MapToolWidgetView : ChartToolWidgetViewBase() {

    private var drawAsCircle: Boolean = false

    override suspend fun getPopulatedView(
        context: Context,
        prefs: WidgetPreferences?
    ): RemoteViews {
        val location = SensorSubsystem.getInstance(context).lastKnownLocation
        val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()
        val views = getView(context, prefs)

        onMain {
            val size = Resources.dp(context, 400f).toInt()
            val cornerRadius = Resources.dp(context, 8f)
            val mapView = MapView(context)
            mapView.clipPath = Path().apply {
                if (drawAsCircle) {
                    addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
                } else {
                    addRoundRect(
                        0f,
                        0f,
                        size.toFloat(),
                        size.toFloat(),
                        FloatArray(8) { cornerRadius },
                        Path.Direction.CW
                    )
                }
            }
            mapView.isInteractive = false
            mapView.isWidget = true
            mapView.mapCenter = location
            mapView.mapAzimuth = 0f
            mapView.userLocation = location
            mapView.resolution = 2.5f

            val layerIds =
                repo.getActiveLayerIds(MapToolRegistration.MAP_ID) + listOf(ScaleBarLayer.LAYER_ID)
            mapView.setLayersWithPreferences(MapToolRegistration.MAP_ID, layerIds)

            if (drawAsCircle) {
                mapView.projection = AzimuthalEquidistantProjection(location)
                mapView.metersPerProjectedUnit = 1.0
                mapView.latitudeScaleFactor = { 1f }
                // TODO: Do something with the scale bar (center it or render it like the radar compass)
            }

            // Set mapview size
            mapView.measure(
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
            )
            mapView.layout(0, 0, size, size)

            mapView.layerManager.start()

            // Initial draw
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            mapView.draw(canvas)
            canvas.drawColor(Color.TRANSPARENT)

            // Wait for map to load and redraw
            delay(2000)
            mapView.draw(canvas)
            views.setImageViewBitmap(CHART, bitmap)
            mapView.layerManager.stop()
        }

        views.setViewVisibility(TITLE_TEXTVIEW, View.GONE)
        views.setOnClickPendingIntent(
            ROOT,
            NavigationUtils.toolPendingIntent(context, Tools.MAP)
        )
        return views
    }
}
