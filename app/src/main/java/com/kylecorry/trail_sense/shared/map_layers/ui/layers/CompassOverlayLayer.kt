package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView

// TODO: Allow setting of position
class CompassOverlayLayer : ILayer {

    var backgroundColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }
    var cardinalDirectionColor: Int = AppColor.Orange.color
        set(value) {
            field = value
            invalidate()
        }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        val compassSize = drawer.dp(24f)
        val arrowWidth = drawer.dp(5f)
        val arrowMargin = drawer.dp(3f)
        val location = PixelCoordinate(
            drawer.canvas.width - drawer.dp(32f),
            drawer.dp(32f)
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

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }
}