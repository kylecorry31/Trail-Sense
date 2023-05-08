package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.EnhancedImageView
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import kotlin.math.max
import kotlin.math.min

class MapCalibrationView : EnhancedImageView {

    var onMapClick: ((percent: PercentCoordinate) -> Unit)? = null
    private var map: PhotoMap? = null
    private val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun setup() {
        super.setup()
        setBackgroundColor(Resources.color(context, R.color.colorSecondary))
    }

    override fun postDraw() {
        super.postDraw()
        drawCalibrationPoints()
    }

    fun showMap(map: PhotoMap) {
        this.map = map
        setImage(map.filename, map.calibration.rotation)
    }

    override fun onSinglePress(e: MotionEvent) {
        super.onSinglePress(e)
        val pixel = toSource(e.x, e.y, true)

        pixel?.let {
            val percentX = it.x / imageWidth
            val percentY = it.y / imageHeight
            val percent = PercentCoordinate(percentX, percentY)
            onMapClick?.invoke(percent)
        }
    }

    // TODO: Support dragging the coordinates

    private fun drawCalibrationPoints() {
        val calibrationPoints = map?.calibration?.calibrationPoints ?: emptyList()
        for (i in calibrationPoints.indices) {
            val point = calibrationPoints[i]
            val sourceCoord = point.imageLocation.toPixels(
                imageWidth.toFloat(),
                imageHeight.toFloat()
            )
            val coord = toView(sourceCoord.x, sourceCoord.y) ?: continue
            drawer.stroke(Color.WHITE)
            drawer.strokeWeight(drawer.dp(1f) / layerScale)
            drawer.fill(Color.BLACK)
            drawer.circle(coord.x, coord.y, drawer.dp(8f) / layerScale)

            drawer.textMode(TextMode.Center)
            drawer.fill(Color.WHITE)
            drawer.noStroke()
            drawer.textSize(drawer.dp(5f) / layerScale)
            drawer.text((i + 1).toString(), coord.x, coord.y)
        }
    }
}