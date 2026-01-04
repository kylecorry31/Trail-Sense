package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer

// TODO: Allow setting of position
class CompassOverlayLayer : OverlayLayer() {

    override val layerId: String = LAYER_ID

    private val backgroundColor: Int
    private val cardinalDirectionColor: Int

    init {
        val context = AppServiceRegistry.get<Context>()
        backgroundColor = Resources.color(context, R.color.colorSecondary)
        cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
    }

    var paddingTopDp: Float = 8f
        set(value) {
            field = value
            invalidate()
        }

    var paddingRightDp: Float = 8f
        set(value) {
            field = value
            invalidate()
        }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        val compassSize = drawer.dp(24f)
        val arrowWidth = drawer.dp(5f)
        val arrowMargin = drawer.dp(3f)
        val location = com.kylecorry.andromeda.core.units.PixelCoordinate(
            drawer.canvas.width - drawer.dp(paddingRightDp + compassSize / 2f),
            drawer.dp(paddingTopDp + compassSize / 2f)
        )
        drawer.push()
        drawer.rotate(-map.mapAzimuth, location.x, location.y)

        // Background circle
        drawer.noTint()
        drawer.fill(backgroundColor)
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(drawer.dp(1f))
        drawer.circle(location.x, location.y, compassSize)

        // Top triangle
        drawer.noStroke()
        drawer.fill(cardinalDirectionColor)
        drawer.triangle(
            location.x,
            location.y - compassSize / 2f + arrowMargin,
            location.x - arrowWidth / 2f,
            location.y,
            location.x + arrowWidth / 2f,
            location.y
        )

        // Bottom triangle
        drawer.fill(Color.WHITE)
        drawer.triangle(
            location.x,
            location.y + compassSize / 2f - arrowMargin,
            location.x - arrowWidth / 2f,
            location.y,
            location.x + arrowWidth / 2f,
            location.y
        )

        drawer.pop()
    }

    companion object {
        const val LAYER_ID = "compass_overlay"
    }
}