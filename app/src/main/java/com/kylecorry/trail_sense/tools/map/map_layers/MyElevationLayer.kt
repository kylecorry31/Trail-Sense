package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MyElevationLayer : OverlayLayer() {

    private var bottomLeft = PixelCoordinate(
        16f,
        -16f
    )

    private val formatter = AppServiceRegistry.get<FormatService>()
    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val locationSubsystem = AppServiceRegistry.get<LocationSubsystem>()

    private val onElevationChange = { _: Bundle ->
        elevation = locationSubsystem.elevation.convertTo(prefs.baseDistanceUnits)
        true
    }

    override val layerId: String = LAYER_ID

    var elevation = Distance.meters(0f)
        set(value) {
            field = value
            elevationString = formatter.formatDistance(value)
        }

    private var elevationString = ""

    private lateinit var bitmapLoader: DrawerBitmapLoader

    override fun start() {
        Tools.subscribe(SensorsToolRegistration.BROADCAST_ELEVATION_CHANGED, onElevationChange)
    }

    override fun drawOverlay(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (!::bitmapLoader.isInitialized) {
            bitmapLoader = DrawerBitmapLoader(drawer)
        }

        val elevationIcon = bitmapLoader.load(R.drawable.ic_altitude, drawer.sp(20f).toInt())

        drawer.push()
        drawer.translate(drawer.dp(bottomLeft.x), drawer.canvas.height + drawer.dp(bottomLeft.y))
        drawer.textMode(TextMode.Corner)
        drawer.textSize(drawer.sp(12f))
        drawer.strokeWeight(4f)
        drawer.stroke(Color.BLACK)
        drawer.fill(Color.WHITE)

        drawer.tint(Color.BLACK)
        drawer.imageMode(ImageMode.Corner)
        drawer.image(elevationIcon, 0f, -elevationIcon.height / 2f)
        drawer.tint(Color.WHITE)
        drawer.image(
            elevationIcon,
            4f,
            -elevationIcon.height / 2f + 4f,
            elevationIcon.width.toFloat() - 8f,
            elevationIcon.height.toFloat() - 8f
        )
        drawer.noTint()

        drawer.text(
            elevationString,
            elevationIcon.width + drawer.dp(4f),
            drawer.textHeight(elevationString) / 2
        )
        drawer.pop()
    }

    override fun stop() {
        Tools.unsubscribe(SensorsToolRegistration.BROADCAST_ELEVATION_CHANGED, onElevationChange)
        if (::bitmapLoader.isInitialized) {
            bitmapLoader.clear()
        }
    }

    companion object {
        const val LAYER_ID = "my_elevation"
    }
}