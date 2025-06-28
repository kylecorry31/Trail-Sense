package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.paths.ui.DistanceScale


class PhotoMapView : BasePhotoMapView {

    var onMapLongClick: ((coordinate: Coordinate) -> Unit)? = null

    private val prefs by lazy { UserPreferences(context) }
    private val units by lazy { prefs.baseDistanceUnits }
    private val formatService by lazy { FormatService.getInstance(context) }
    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    private var cardinalDirectionColor: Int = Color.WHITE

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun drawOverlay() {
        super.drawOverlay()
        drawScale()
        drawCompass()
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        val viewNoRotation = toViewNoRotation(PointF(e.x, e.y)) ?: return
        val coordinate = toCoordinate(toPixel(viewNoRotation))
        onMapLongClick?.invoke(coordinate)
    }

    override fun onSinglePress(e: MotionEvent) {
        super.onSinglePress(e)
        val viewNoRotation = toViewNoRotation(PointF(e.x, e.y))

        // TODO: Pass in a coordinate rather than a pixel (convert radius to meters)
        if (viewNoRotation != null) {
            for (layer in layers.reversed()) {
                val handled = layer.onClick(
                    drawer,
                    this@PhotoMapView,
                    PixelCoordinate(viewNoRotation.x, viewNoRotation.y)
                )
                if (handled) {
                    break
                }
            }
        }
    }

    override fun setup() {
        super.setup()
        cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
    }

    // TODO: Extract this (same way as scale)
    private fun drawCompass() {
        val compassSize = drawer.dp(24f)
        val arrowWidth = drawer.dp(5f)
        val arrowMargin = drawer.dp(3f)
        val location = PixelCoordinate(
            width - drawer.dp(32f),
            drawer.dp(32f)
        )
        drawer.push()
        drawer.rotate(-mapAzimuth, location.x, location.y)

        // Background circle
        drawer.noTint()
        drawer.fill(Resources.color(context, R.color.colorSecondary))
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

    // TODO: Extract this to either a base mapview class, layer, or helper class
    private fun drawScale() {
        drawer.noFill()
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)

        val scaleSize = distanceScale.getScaleDistance(units, width / 2f, metersPerPixel)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, metersPerPixel, scaleBar)
        val start = width - drawer.dp(16f) - drawer.pathWidth(scaleBar)
        val y = height - drawer.dp(16f)
        drawer.push()
        drawer.translate(start, y)
        drawer.stroke(Color.BLACK)
        drawer.strokeWeight(8f)
        drawer.path(scaleBar)
        drawer.stroke(Color.WHITE)
        drawer.strokeWeight(4f)
        drawer.path(scaleBar)
        drawer.pop()

        drawer.textMode(TextMode.Corner)
        drawer.textSize(drawer.sp(12f))
        drawer.strokeWeight(4f)
        drawer.stroke(Color.BLACK)
        drawer.fill(Color.WHITE)
        val scaleText =
            formatService.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        drawer.text(
            scaleText,
            start - drawer.textWidth(scaleText) - drawer.dp(4f),
            y + drawer.textHeight(scaleText) / 2
        )
    }

}