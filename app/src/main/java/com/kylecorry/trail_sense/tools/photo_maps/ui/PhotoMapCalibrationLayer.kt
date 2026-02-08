package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.photo_maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap

class PhotoMapCalibrationLayer(
    private val getMap: () -> PhotoMap?,
    private val getHighlightedIndex: () -> Int,
    private val onPointMoved: (PercentCoordinate) -> Unit
) : ILayer {

    override val layerId: String = LAYER_ID

    private var highlightedColor: Int? = null

    override fun setPreferences(preferences: Bundle) {
        // Do nothing
    }

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        val photoMap = getMap() ?: return
        val points = photoMap.calibration.calibrationPoints
        if (points.isEmpty()) {
            return
        }

        if (highlightedColor == null) {
            highlightedColor = Resources.getPrimaryMarkerColor(context)
        }

        val highlightedIndex = getHighlightedIndex()
        val scale = map.layerScale
        val radius = drawer.dp(12f) / scale
        val stroke = drawer.dp(1f) / scale
        val textSize = drawer.dp(10f) / scale

        val orderedPoints = points.mapIndexed { index, point -> index to point }
            .sortedBy { it.first == highlightedIndex }

        for ((index, point) in orderedPoints) {
            if (point.location == com.kylecorry.sol.units.Coordinate.zero && index != highlightedIndex) {
                continue
            }

            val pixel = map.toPixel(point.location)
            if (pixel.x.isNaN() || pixel.y.isNaN()) {
                continue
            }

            drawer.stroke(Color.WHITE)
            if (index == highlightedIndex) {
                drawer.fill(highlightedColor ?: Color.WHITE)
            } else {
                drawer.fill(Color.BLACK)
            }
            drawer.strokeWeight(stroke)
            drawer.circle(pixel.x, pixel.y, radius)

            drawer.textMode(com.kylecorry.andromeda.canvas.TextMode.Center)
            if (index == highlightedIndex) {
                drawer.fill(Color.BLACK)
            } else {
                drawer.fill(Color.WHITE)
            }
            drawer.noStroke()
            drawer.textSize(textSize)
            drawer.text((index + 1).toString(), pixel.x, pixel.y)
        }
    }

    override fun drawOverlay(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        val photoMap = getMap() ?: return false
        if (!photoMap.isCalibrated) {
            return false
        }

        val location = map.toCoordinate(pixel)
        val projection = photoMap.projection
        val imagePixel = projection.toPixels(location)
        val size = photoMap.unrotatedSize()
        if (size.width <= 0f || size.height <= 0f) {
            return false
        }

        val percentX = imagePixel.x / size.width
        val percentY = imagePixel.y / size.height

        if (percentX.isNaN() || percentY.isNaN()) {
            return false
        }

        if (percentX < 0f || percentX > 1f || percentY < 0f || percentY > 1f) {
            return false
        }

        onPointMoved(PercentCoordinate(percentX, percentY))
        return true
    }

    override var percentOpacity: Float = 1f

    companion object {
        const val LAYER_ID = "photo_map_calibration"
    }
}
