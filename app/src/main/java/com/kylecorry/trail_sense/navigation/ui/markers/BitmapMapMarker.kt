package com.kylecorry.trail_sense.navigation.ui.markers

import android.graphics.Bitmap
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

class BitmapMapMarker(
    override val location: Coordinate,
    private val bitmap: Bitmap,
    override val size: Float = 12f,
    private val rotation: Float? = null,
    private val tint: Int? = null,
    private val onClickFn: () -> Boolean = { false }
) : MapMarker {
    override fun draw(
        drawer: ICanvasDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float
    ) {
        val size = drawer.dp(this.size) * scale
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        var finalWidth = size
        var finalHeight = size
        if (aspectRatio < 1f) {
            finalWidth = size * aspectRatio
        } else {
            finalHeight = size / aspectRatio
        }
        drawer.imageMode(ImageMode.Center)
        drawer.push()
        if (tint != null){
            drawer.tint(tint)
        } else {
            drawer.noTint()
        }
        drawer.rotate(this.rotation ?: rotation, anchor.x, anchor.y)
        drawer.image(bitmap, anchor.x, anchor.y, finalWidth, finalHeight)
        drawer.pop()
        drawer.imageMode(ImageMode.Corner)
        drawer.noTint()
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }
}