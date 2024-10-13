package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader

class MyElevationLayer(
    private val formatter: FormatService,
    private val bottomLeft: PixelCoordinate,
) : ILayer {

    var elevation = Distance.meters(0f)
        set(value) {
            field = value
            elevationString = formatter.formatDistance(value)
        }

    private var elevationString = ""

    private lateinit var bitmapLoader: DrawerBitmapLoader

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
        if (!::bitmapLoader.isInitialized) {
            bitmapLoader = DrawerBitmapLoader(drawer)
        }

        val elevationIcon = bitmapLoader.load(R.drawable.ic_altitude, drawer.sp(20f).toInt())

        drawer.push()
        drawer.translate(bottomLeft.x, drawer.canvas.height + bottomLeft.y)
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

    protected fun finalize() {
        bitmapLoader.clear()
    }
}